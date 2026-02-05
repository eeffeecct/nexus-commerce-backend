package ru.nexus.inventory.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nexus.inventory.dto.InventoryResponse;
import ru.nexus.inventory.entity.Inventory;
import ru.nexus.inventory.repository.InventoryRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public InventoryResponse isInStock(String skuCode) {
        return inventoryRepository.findBySkuCode(skuCode)
                .map(inventory -> InventoryResponse.builder()
                        .skuCode(inventory.getSkuCode())
                        .isInStock(inventory.getQuantity() > 0)
                        .quantity(inventory.getQuantity())
                        .build())
                .orElse(InventoryResponse.builder()
                        .skuCode(skuCode)
                        .isInStock(false)
                        .quantity(0)
                        .build());
    }

    @Transactional
    public void initStock(String skuCode) {
        if (inventoryRepository.existsBySkuCode(skuCode)) {
            log.warn("Stock for skuCode {} already exists", skuCode);
            return;
        }
        inventoryRepository.save(Inventory.builder()
                .skuCode(skuCode)
                .quantity(0)
                .build());
    }
}
