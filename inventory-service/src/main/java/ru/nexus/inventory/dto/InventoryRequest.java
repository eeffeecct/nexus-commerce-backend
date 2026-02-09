package ru.nexus.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryRequest {
    @NotBlank(message = "SKU code is required")
    private String skuCode;

    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    private Integer version;
}