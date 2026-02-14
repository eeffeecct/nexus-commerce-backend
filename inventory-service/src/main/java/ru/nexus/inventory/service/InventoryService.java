package ru.nexus.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nexus.inventory.dto.InventoryRequest;
import ru.nexus.inventory.dto.InventoryResponse;
import ru.nexus.inventory.entity.Inventory;
import ru.nexus.inventory.exception.InsufficientStockException;
import ru.nexus.inventory.exception.InventoryNotFoundException;
import ru.nexus.inventory.repository.InventoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .isInStock(inventory.getQuantity() > 0)
                .quantity(inventory.getQuantity())
                .version(inventory.getVersion())
                .build();
    }

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
    public InventoryResponse getInventoryDetails(String skuCode) {
        log.info("Get inventory details for: {}", skuCode);
        return inventoryRepository.findBySkuCode(skuCode)
                .map(this::mapToResponse)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory record missing for: " + skuCode));
    }

    @Transactional
    public List<InventoryResponse> getStockStatuses(List<String> skuCodes) {
        log.info("Checking stock statuses for: {}", skuCodes);
        return inventoryRepository.findAllBySkuCodes(skuCodes).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public void checkAvailability(List<InventoryRequest> requestList) {
        List<String> skuCodes = requestList.stream()
                .map(InventoryRequest::getSkuCode)
                .toList();

        long uniqueRequestedCount = requestList.stream().map(InventoryRequest::getSkuCode).distinct().count();

        List<Inventory> inventoryList = inventoryRepository.findAllBySkuCodes(skuCodes);

        if (inventoryList.size() != uniqueRequestedCount) {
            throw new InventoryNotFoundException("Some products are missing in inventory");
        }

        for (InventoryRequest request : requestList) {
            Inventory stock = inventoryList.stream()
                    .filter(inv -> inv.getSkuCode().equals(request.getSkuCode()))
                    .findFirst()
                    .orElseThrow(() -> new InventoryNotFoundException("SKU not found: " + request.getSkuCode()));
            if (stock.getQuantity() < request.getQuantity()) {
                throw new InsufficientStockException("Not enough stock for SKU: " + request.getSkuCode());
            }
        }
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
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for skuCode: " + request.getSkuCode()));

        inventory.setQuantity(request.getQuantity());
        inventory.setVersion(request.getVersion());

        inventoryRepository.saveInventory(inventory);
    }

    @Transactional
    public void adjustStock(String skuCode, Integer delta) {
        log.info("Adjusting stock for skuCode: {} by delta: {}", skuCode, delta);

        int rowsAffected = inventoryRepository.updateQuantity(skuCode, delta);

        if (rowsAffected > 0) {
            return;
        }

        log.warn("Stock adjustment failed. Diagnosing cause for sku: {}", skuCode);

        boolean exists = inventoryRepository.existsBySkuCode(skuCode);

        if (!exists) {
            throw new InventoryNotFoundException("Inventory not found for sku: " + skuCode);
        } else {
            throw new InsufficientStockException("Insufficient stock for sku: " + skuCode);
        }
    }

    @Transactional
    public void reserveStock(List<InventoryRequest> requestList, Integer delta) {
        log.info("Reserving stock for items: {} ", requestList);
        for (InventoryRequest request : requestList) {
            adjustStock(request.getSkuCode(), -request.getQuantity());
        }
    }

    @Transactional
    public void deleteInventory(String skuCode) {
        log.info("Deleting inventory for skuCode: {}", skuCode);
        long rowsDeleted = inventoryRepository.deleteBySkuCode(skuCode);
        if (rowsDeleted == 0) {
            throw new InventoryNotFoundException("Inventory not found for sku: " + skuCode);
        }
    }
}