package com.guvi.ecommerce.service;

import com.guvi.ecommerce.dto.OrderResponse;
import com.guvi.ecommerce.entity.*;
import com.guvi.ecommerce.exception.BadRequestException;
import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.repository.*;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public List<OrderResponse> getUserOrders(String email) {
        User user = getUser(email);
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createOrder(String email) throws RazorpayException {
        User user = getUser(email);
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Re-check stock at checkout time — it may have changed since the item was added.
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product.getStock() == null || product.getStock() < item.getQuantity()) {
                throw new BadRequestException("Only " + (product.getStock() == null ? 0 : product.getStock())
                        + " unit(s) of " + product.getName() + " are in stock");
            }
        }

        BigDecimal total = cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Razorpay expects the amount in paise, as an integer.
        int amountInPaise = total.multiply(BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP).intValueExact();

        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());
        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

        List<OrderItem> orderItems = cartItems.stream().map(ci -> OrderItem.builder()
                .product(ci.getProduct())
                .quantity(ci.getQuantity())
                .price(ci.getProduct().getPrice())
                .build()).collect(Collectors.toList());

        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .status(Order.OrderStatus.CREATED)
                .razorpayOrderId(razorpayOrder.get("id"))
                .build();
        order = orderRepository.save(order);

        final Order savedOrder = order;
        orderItems.forEach(item -> item.setOrder(savedOrder));
        savedOrder.setItems(orderItems);
        orderRepository.save(savedOrder);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", savedOrder.getId());
        response.put("razorpayOrderId", razorpayOrder.get("id"));
        response.put("amount", amountInPaise);
        response.put("currency", "INR");
        response.put("keyId", razorpayKeyId);
        return response;
    }

    @Transactional
    public OrderResponse verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Verify signature
        String generatedSignature = hmacSHA256(razorpayOrderId + "|" + razorpayPaymentId, razorpayKeySecret);
        if (!generatedSignature.equals(razorpaySignature)) {
            order.setStatus(Order.OrderStatus.FAILED);
            orderRepository.save(order);
            throw new BadRequestException("Payment verification failed");
        }

        if (order.getStatus() == Order.OrderStatus.PAID) {
            // Razorpay can deliver the same confirmation twice — don't deduct stock again.
            return OrderResponse.from(order);
        }

        order.setStatus(Order.OrderStatus.PAID);
        order.setRazorpayPaymentId(razorpayPaymentId);
        Order saved = orderRepository.save(order);

        deductStock(saved);

        // Clear cart
        cartItemRepository.deleteByUser(order.getUser());
        return OrderResponse.from(saved);
    }

    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        try {
            order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown order status: " + status);
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    private void deductStock(Order order) {
        if (order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int remaining = (product.getStock() == null ? 0 : product.getStock()) - item.getQuantity();
            product.setStock(Math.max(remaining, 0));
            productRepository.save(product);
        }
    }

    private String hmacSHA256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
