package com.guvi.ecommerce.service;

import com.guvi.ecommerce.dto.CartItemResponse;
import com.guvi.ecommerce.dto.CartRequest;
import com.guvi.ecommerce.entity.*;
import com.guvi.ecommerce.exception.BadRequestException;
import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartItemRepository cartItemRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;

    @InjectMocks CartService cartService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("buyer@test.com").name("Buyer")
                .role(User.Role.CUSTOMER).build();
        product = Product.builder().id(2L).name("Lamp")
                .price(BigDecimal.valueOf(500)).stock(4).build();
    }

    private CartRequest request(int quantity) {
        CartRequest req = new CartRequest();
        req.setProductId(2L);
        req.setQuantity(quantity);
        return req;
    }

    @Test
    void getCart_mapsToResponseWithSubtotal() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUser(user)).thenReturn(List.of(
                CartItem.builder().id(9L).user(user).product(product).quantity(3).build()));

        List<CartItemResponse> cart = cartService.getCart("buyer@test.com");

        assertThat(cart).hasSize(1);
        assertThat(cart.get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(cart.get(0).getProduct().getName()).isEqualTo("Lamp");
    }

    @Test
    void addToCart_newItem_savesWithRequestedQuantity() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemResponse result = cartService.addToCart("buyer@test.com", request(2));

        assertThat(result.getQuantity()).isEqualTo(2);
    }

    @Test
    void addToCart_existingItem_accumulatesQuantity() {
        CartItem existing = CartItem.builder().id(9L).user(user).product(product).quantity(1).build();
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemResponse result = cartService.addToCart("buyer@test.com", request(2));

        assertThat(result.getQuantity()).isEqualTo(3);
    }

    @Test
    void addToCart_beyondAvailableStock_isRejected() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart("buyer@test.com", request(5)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("in stock");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addToCart_accumulationCrossingStockLimit_isRejected() {
        CartItem existing = CartItem.builder().id(9L).user(user).product(product).quantity(3).build();
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.addToCart("buyer@test.com", request(2)))
                .isInstanceOf(BadRequestException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addToCart_unknownProduct_throwsNotFound() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart("buyer@test.com", request(1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCartItem_zeroQuantity_isRejected() {
        assertThatThrownBy(() -> cartService.updateCartItem("buyer@test.com", 9L, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 1");
    }

    @Test
    void updateCartItem_anotherUsersItem_isDenied() {
        User other = User.builder().id(99L).email("other@test.com").build();
        CartItem item = CartItem.builder().id(9L).user(other).product(product).quantity(1).build();
        when(cartItemRepository.findById(9L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateCartItem("buyer@test.com", 9L, 1))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void removeFromCart_ownedItem_isDeleted() {
        CartItem item = CartItem.builder().id(9L).user(user).product(product).quantity(1).build();
        when(cartItemRepository.findById(9L)).thenReturn(Optional.of(item));

        cartService.removeFromCart("buyer@test.com", 9L);

        verify(cartItemRepository).delete(item);
    }
}
