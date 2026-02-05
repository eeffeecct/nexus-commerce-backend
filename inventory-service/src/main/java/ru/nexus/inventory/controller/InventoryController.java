package ru.nexus.inventory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.nexus.inventory.dto.InventoryResponse;
import ru.nexus.inventory.service.InventoryService;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{skuCode}")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse isInStock(@PathVariable String skuCode) {
        return inventoryService.isInStock(skuCode);
    }

    // Временный эндпоинт для тестов, позже уберем и заменим на EventListener
    @PostMapping("/{skuCode}")
    @ResponseStatus(HttpStatus.CREATED)
    public void initStock(@PathVariable String skuCode) {
        inventoryService.initStock(skuCode);
    }
}