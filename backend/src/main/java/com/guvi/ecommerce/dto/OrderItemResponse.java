package com.guvi.ecommerce.dto;

import com.guvi.ecommerce.entity.OrderItem;
import com.guvi.ecommerce.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Product product;
    private Integer quantity;
    private BigDecimal price;

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getProduct(),
                item.getQuantity(), item.getPrice());
    }
}
