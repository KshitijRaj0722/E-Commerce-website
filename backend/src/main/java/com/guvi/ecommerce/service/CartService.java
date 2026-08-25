package com.guvi.ecommerce.service;

import com.guvi.ecommerce.dto.CartItemResponse;
import com.guvi.ecommerce.dto.CartRequest;
import com.guvi.ecommerce.entity.*;
import com.guvi.ecommerce.exception.BadRequestException;
import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart(String email) {
        User user = getUser(email);
        return cartItemRepository.findByUser(user).stream()
                .map(CartItemResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CartItemResponse addToCart(String email, CartRequest request) {
        User user = getUser(email);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        CartItem item = cartItemRepository.findByUserAndProduct(user, product)
                .orElseGet(() -> CartItem.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .build());

        int newQuantity = item.getQuantity() + request.getQuantity();
        requireStock(product, newQuantity);
        item.setQuantity(newQuantity);
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public CartItemResponse updateCartItem(String email, Long itemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        CartItem item = getOwnedItem(email, itemId);
        requireStock(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public void removeFromCart(String email, Long itemId) {
        cartItemRepository.delete(getOwnedItem(email, itemId));
    }

    @Transactional
    public void clearCart(String email) {
        User user = getUser(email);
        cartItemRepository.deleteByUser(user);
    }

    private CartItem getOwnedItem(String email, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!item.getUser().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "This cart item belongs to another user");
        }
        return item;
    }

    private void requireStock(Product product, int requested) {
        if (product.getStock() == null || product.getStock() < requested) {
            throw new BadRequestException("Only " + (product.getStock() == null ? 0 : product.getStock())
                    + " unit(s) of " + product.getName() + " are in stock");
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
