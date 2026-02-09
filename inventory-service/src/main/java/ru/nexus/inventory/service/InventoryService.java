package ru.nexus.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
    public InventoryResponse getStockStatus(String skuCode) {
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
        try {
            Inventory inventory = Inventory.builder()
                    .skuCode(skuCode)
                    .quantity(0)
                    .build();
            inventoryRepository.save(inventory);
        } catch (DuplicateKeyException e) {
            log.warn("Stock for skuCode {} already exists (Race Condition avoided)", skuCode);
        }
    }
}
