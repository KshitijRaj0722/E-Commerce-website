package com.guvi.ecommerce.controller;

import com.guvi.ecommerce.config.JwtUtil;
import com.guvi.ecommerce.dto.*;
import com.guvi.ecommerce.entity.Product;
import com.guvi.ecommerce.exception.BadRequestException;
import com.guvi.ecommerce.service.OrderService;
import com.guvi.ecommerce.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private OrderService orderService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private OrderResponse sampleOrder() {
        Product product = Product.builder().id(1L).name("Lamp")
                .price(BigDecimal.valueOf(500)).stock(4).build();
        OrderItemResponse item = new OrderItemResponse(10L, product, 2, BigDecimal.valueOf(500));
        return new OrderResponse(7L, new UserSummary(3L, "Buyer", "buyer@test.com"),
                BigDecimal.valueOf(1000), "PAID", "order_ABC", "pay_XYZ",
                LocalDateTime.now(), List.of(item));
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
    void getMyOrders_serializesWithoutRecursionOrPasswordLeak() throws Exception {
        when(orderService.getUserOrders("buyer@test.com")).thenReturn(List.of(sampleOrder()));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].items.length()").value(1))
                .andExpect(jsonPath("$[0].items[0].product.name").value("Lamp"))
                // the nested item must NOT carry a back-reference to its order
                .andExpect(jsonPath("$[0].items[0].order").doesNotExist())
                .andExpect(jsonPath("$[0].user.email").value("buyer@test.com"))
                .andExpect(jsonPath("$[0].user.password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
    void checkout_returnsRazorpayHandoffPayload() throws Exception {
        when(orderService.createOrder("buyer@test.com")).thenReturn(Map.of(
                "orderId", 7L,
                "razorpayOrderId", "order_ABC",
                "amount", 100000,
                "currency", "INR",
                "keyId", "rzp_test_key"));

        mockMvc.perform(post("/api/orders/checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_ABC"))
                .andExpect(jsonPath("$.amount").value(100000))
                .andExpect(jsonPath("$.keyId").value("rzp_test_key"));
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
    void checkout_emptyCart_returns400WithMessage() throws Exception {
        when(orderService.createOrder("buyer@test.com")).thenThrow(new BadRequestException("Cart is empty"));

        mockMvc.perform(post("/api/orders/checkout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart is empty"));
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
    void verifyPayment_returnsPaidOrder() throws Exception {
        when(orderService.verifyPayment("buyer@test.com", "order_ABC", "pay_XYZ", "sig")).thenReturn(sampleOrder());

        mockMvc.perform(post("/api/orders/verify-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson("order_ABC", "pay_XYZ", "sig")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.razorpayPaymentId").value("pay_XYZ"));
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
    void verifyPayment_badSignature_returns400() throws Exception {
        when(orderService.verifyPayment(any(), any(), any(), any()))
                .thenThrow(new BadRequestException("Payment verification failed"));

        mockMvc.perform(post("/api/orders/verify-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson("order_ABC", "pay_XYZ", "tampered")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment verification failed"));
    }

    private static String paymentJson(String orderId, String paymentId, String signature) {
        return String.format(
                "{\"razorpayOrderId\":\"%s\",\"razorpayPaymentId\":\"%s\",\"razorpaySignature\":\"%s\"}",
                orderId, paymentId, signature);
    }
}
