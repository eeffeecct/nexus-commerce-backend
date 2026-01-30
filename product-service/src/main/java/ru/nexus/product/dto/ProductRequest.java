package ru.nexus.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    @NotBlank
    private String title;
    @NotNull(message = "Price must be not null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;
    @NotNull(message = "Quantity must be not null")
    @Min(value = 0, message = "Quantity must be positive")
    private Integer quantity;
    @NotBlank(message = "Category must be specified")
    private String category;
    private Map<String, Object> attributes;
}
