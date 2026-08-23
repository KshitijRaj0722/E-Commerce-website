package com.guvi.ecommerce.dto;

import com.guvi.ecommerce.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private UserSummary user;
    private BigDecimal totalAmount;
    private String status;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems() == null
                ? Collections.emptyList()
                : order.getItems().stream().map(OrderItemResponse::from).collect(Collectors.toList());
        return new OrderResponse(
                order.getId(),
                UserSummary.from(order.getUser()),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getRazorpayOrderId(),
                order.getRazorpayPaymentId(),
                order.getCreatedAt(),
                items);
    }
}
