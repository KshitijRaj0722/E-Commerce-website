package com.guvi.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CartRequest {
    @NotNull
    private Long productId;
    @NotNull @Min(1)
    private Integer quantity;
}
