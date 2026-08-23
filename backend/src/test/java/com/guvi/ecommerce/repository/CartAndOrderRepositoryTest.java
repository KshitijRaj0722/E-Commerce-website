package com.guvi.ecommerce.repository;

import com.guvi.ecommerce.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CartAndOrderRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;

    private User user;
    private Product product;

    @BeforeEach
    void seed() {
        user = userRepository.save(User.builder().name("Buyer").email("buyer@test.com")
                .password("hashed").role(User.Role.CUSTOMER).build());
        product = productRepository.save(Product.builder().name("Lamp")
                .price(BigDecimal.valueOf(500)).stock(8).build());
    }

    @Test
    void findByUserAndProduct_locatesExistingCartLine() {
        cartItemRepository.save(CartItem.builder().user(user).product(product).quantity(2).build());

        assertThat(cartItemRepository.findByUserAndProduct(user, product))
                .isPresent()
                .get()
                .extracting(CartItem::getQuantity)
                .isEqualTo(2);
    }

    @Test
    void deleteByUser_emptiesOnlyThatUsersCart() {
        User other = userRepository.save(User.builder().name("Other").email("other@test.com")
                .password("hashed").role(User.Role.CUSTOMER).build());
        cartItemRepository.save(CartItem.builder().user(user).product(product).quantity(1).build());
        cartItemRepository.save(CartItem.builder().user(other).product(product).quantity(4).build());

        cartItemRepository.deleteByUser(user);

        assertThat(cartItemRepository.findByUser(user)).isEmpty();
        assertThat(cartItemRepository.findByUser(other)).hasSize(1);
    }

    @Test
    void findByRazorpayOrderId_returnsMatchingOrder() {
        orderRepository.save(Order.builder().user(user).totalAmount(BigDecimal.valueOf(500))
                .status(Order.OrderStatus.CREATED).razorpayOrderId("order_ABC123").build());

        assertThat(orderRepository.findByRazorpayOrderId("order_ABC123")).isPresent();
        assertThat(orderRepository.findByRazorpayOrderId("order_NOPE")).isEmpty();
    }

    @Test
    void findByUserOrderByCreatedAtDesc_returnsOnlyThatUsersOrders() {
        orderRepository.save(Order.builder().user(user).totalAmount(BigDecimal.TEN)
                .status(Order.OrderStatus.CREATED).razorpayOrderId("o1").build());
        orderRepository.save(Order.builder().user(user).totalAmount(BigDecimal.ONE)
                .status(Order.OrderStatus.PAID).razorpayOrderId("o2").build());

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);

        assertThat(orders).hasSize(2);
    }
}
