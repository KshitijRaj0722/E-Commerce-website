package com.guvi.ecommerce.controller;

import com.guvi.ecommerce.dto.OrderResponse;
import com.guvi.ecommerce.dto.PaymentVerifyRequest;
import com.guvi.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(orderService.getUserOrders(user.getUsername()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@AuthenticationPrincipal UserDetails user) throws Exception {
        return ResponseEntity.ok(orderService.createOrder(user.getUsername()));
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<OrderResponse> verifyPayment(@AuthenticationPrincipal UserDetails user,
                                                       @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(orderService.verifyPayment(
                user.getUsername(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/admin/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long orderId, @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }
}
