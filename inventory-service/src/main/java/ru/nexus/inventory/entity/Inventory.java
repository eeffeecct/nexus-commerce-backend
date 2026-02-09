package ru.nexus.inventory.entity;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Inventory {
    private Long id;
    private String skuCode;
    private Integer quantity;
    private Integer version;
}