package ru.nexus.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nexus.inventory.dto.InventoryRequest;
import ru.nexus.inventory.dto.InventoryResponse;
import ru.nexus.inventory.entity.Inventory;
import ru.nexus.inventory.repository.InventoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public InventoryResponse getStockStatus(String skuCode) {
        return inventoryRepository.findBySkuCode(skuCode)
                .map(this::mapToResponse)
                .orElse(InventoryResponse.builder()
                        .skuCode(skuCode)
                        .isInStock(false)
                        .quantity(0)
                        .version(0)
                        .build());
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getStockStatuses(List<String> skuCodes) {
        log.info("Checking stock statuses for: {}", skuCodes);
        return inventoryRepository.findAllBySkuCodeIn(skuCodes).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void initStock(String skuCode) {
        try {
            Inventory inventory = Inventory.builder()
                    .skuCode(skuCode)
                    .quantity(0)
                    .version(0)
                    .build();
            inventoryRepository.saveInventory(inventory);
        } catch (DuplicateKeyException e) {
            log.warn("Stock for skuCode {} already exists (Race Condition avoided)", skuCode);
        }
    }

    @Transactional
    public void updateInventory(InventoryRequest request) {
        log.info("Updating inventory for skuCode: {}", request.getSkuCode());
        
        Inventory inventory = inventoryRepository.findBySkuCode(request.getSkuCode())
                .orElseThrow(() -> new RuntimeException("Inventory not found for skuCode: " + request.getSkuCode()));

        inventory.setQuantity(request.getQuantity());
        inventory.setVersion(request.getVersion());

        inventoryRepository.saveInventory(inventory);
    }

    @Transactional
    public void adjustStock(String skuCode, Integer delta) {
        log.info("Adjusting stock for skuCode: {} by delta: {}", skuCode, delta);
        int rowsAffected = inventoryRepository.updateQuantity(skuCode, delta);
        if (rowsAffected == 0) {
            throw new RuntimeException("Stock adjustment failed. Product not found or insufficient quantity.");
        }
    }

    @Transactional
    public void deleteInventory(String skuCode) {
        log.info("Deleting inventory for skuCode: {}", skuCode);
        inventoryRepository.deleteBySkuCode(skuCode);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .isInStock(inventory.getQuantity() > 0)
                .quantity(inventory.getQuantity())
                .version(inventory.getVersion())
                .build();
    }
}