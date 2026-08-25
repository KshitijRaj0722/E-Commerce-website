package com.guvi.ecommerce.service;

import com.guvi.ecommerce.dto.OrderResponse;
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
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String KEY_SECRET = "rzp_test_secret";

    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;

    @InjectMocks OrderService orderService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(orderService, "razorpayKeySecret", KEY_SECRET);
        user = User.builder().id(1L).email("buyer@test.com").name("Buyer")
                .role(User.Role.CUSTOMER).build();
        product = Product.builder().id(2L).name("Lamp")
                .price(BigDecimal.valueOf(500)).stock(10).build();
    }

    /** Mirrors the signature Razorpay's checkout widget sends back to us. */
    private static String sign(String orderId, String paymentId) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY_SECRET.getBytes(), "HmacSHA256"));
        byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private Order orderWithItems(Order.OrderStatus status) {
        Order order = Order.builder().id(7L).user(user).totalAmount(BigDecimal.valueOf(1000))
                .status(status).razorpayOrderId("order_ABC").build();
        order.setItems(List.of(OrderItem.builder().id(11L).order(order).product(product)
                .quantity(3).price(BigDecimal.valueOf(500)).build()));
        return order;
    }

    @Test
    void getUserOrders_mapsEntitiesToResponses() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(orderWithItems(Order.OrderStatus.PAID)));

        List<OrderResponse> orders = orderService.getUserOrders("buyer@test.com");

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo("PAID");
        assertThat(orders.get(0).getUser().getEmail()).isEqualTo("buyer@test.com");
        assertThat(orders.get(0).getItems()).hasSize(1);
    }

    @Test
    void createOrder_emptyCart_isRejectedBeforeCallingRazorpay() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUser(user)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrder("buyer@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cart is empty");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_insufficientStock_isRejectedBeforeCallingRazorpay() {
        product.setStock(1);
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUser(user)).thenReturn(List.of(
                CartItem.builder().id(9L).user(user).product(product).quantity(3).build()));

        assertThatThrownBy(() -> orderService.createOrder("buyer@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("in stock");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void verifyPayment_validSignature_marksPaidDeductsStockAndClearsCart() throws Exception {
        Order order = orderWithItems(Order.OrderStatus.CREATED);
        when(orderRepository.findByRazorpayOrderId("order_ABC")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = orderService.verifyPayment(
                "buyer@test.com", "order_ABC", "pay_XYZ", sign("order_ABC", "pay_XYZ"));

        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(result.getRazorpayPaymentId()).isEqualTo("pay_XYZ");
        assertThat(product.getStock()).isEqualTo(7);   // 10 - 3
        verify(productRepository).save(product);
        verify(cartItemRepository).deleteByUser(user);
    }

    @Test
    void verifyPayment_tamperedSignature_marksFailedAndLeavesStockAlone() {
        Order order = orderWithItems(Order.OrderStatus.CREATED);
        when(orderRepository.findByRazorpayOrderId("order_ABC")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> orderService.verifyPayment("buyer@test.com", "order_ABC", "pay_XYZ", "not-the-signature"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("verification failed");

        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.FAILED);
        assertThat(product.getStock()).isEqualTo(10);
        verify(productRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByUser(any());
    }

    @Test
    void verifyPayment_replayedConfirmation_doesNotDeductStockTwice() throws Exception {
        Order alreadyPaid = orderWithItems(Order.OrderStatus.PAID);
        when(orderRepository.findByRazorpayOrderId("order_ABC")).thenReturn(Optional.of(alreadyPaid));

        OrderResponse result = orderService.verifyPayment(
                "buyer@test.com", "order_ABC", "pay_XYZ", sign("order_ABC", "pay_XYZ"));

        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(product.getStock()).isEqualTo(10);
        verify(productRepository, never()).save(any());
    }

    @Test
    void verifyPayment_anotherUsersOrder_isDeniedWithoutChangingStatus() throws Exception {
        Order order = orderWithItems(Order.OrderStatus.CREATED);
        when(orderRepository.findByRazorpayOrderId("order_ABC")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.verifyPayment(
                "intruder@test.com", "order_ABC", "pay_XYZ", "any-signature"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        // the intruder must not be able to flip somebody else's order to FAILED
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CREATED);
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void verifyPayment_unknownOrder_throwsNotFound() throws Exception {
        when(orderRepository.findByRazorpayOrderId("order_MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.verifyPayment(
                "buyer@test.com", "order_MISSING", "pay_XYZ", sign("order_MISSING", "pay_XYZ")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateOrderStatus_unknownValue_isRejected() {
        when(orderRepository.findById(7L)).thenReturn(Optional.of(orderWithItems(Order.OrderStatus.CREATED)));

        assertThatThrownBy(() -> orderService.updateOrderStatus(7L, "SHIPPED"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown order status");
    }

    @Test
    void updateOrderStatus_validValue_isPersisted() {
        Order order = orderWithItems(Order.OrderStatus.CREATED);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = orderService.updateOrderStatus(7L, "CANCELLED");

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
    }
}
