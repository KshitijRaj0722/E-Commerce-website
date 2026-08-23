package com.guvi.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull @DecimalMin("0.0")
    private BigDecimal price;
    @NotNull @Min(0)
    private Integer stock;
    private String imageUrl;
    private String category;
}
