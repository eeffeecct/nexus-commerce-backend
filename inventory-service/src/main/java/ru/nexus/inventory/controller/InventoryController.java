package ru.nexus.inventory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.nexus.inventory.dto.InventoryRequest;
import ru.nexus.inventory.dto.InventoryResponse;
import ru.nexus.inventory.service.InventoryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // --- КЛИЕНТСКИЙ API (Витрина) ---

    // Статус одного товара
    @GetMapping("/{skuCode}")
    public InventoryResponse getStockStatus(@PathVariable String skuCode) {
        return inventoryService.getStockStatus(skuCode);
    }

    // Массовый статус для каталога
    @GetMapping
    public List<InventoryResponse> getStockStatuses(@RequestParam List<String> skuCodes) {
        return inventoryService.getStockStatuses(skuCodes);
    }

    // --- СЕРВИСНЫЙ API (Для Order Service) ---

    // Проверка корзины перед созданием заказа
    @PostMapping("/check")
    @ResponseStatus(HttpStatus.OK)
    public void checkAvailability(@RequestBody List<InventoryRequest> request) {
        inventoryService.checkAvailability(request);
    }

    // Бронирование товара (списание)
    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.OK)
    public void reserveStock(@RequestBody List<InventoryRequest> request) {
        inventoryService.reserveStock(request);
    }


    // --- АДМИНСКИЙ API ---

    // Точные детали склада
    @GetMapping("/details/{skuCode}")
    public InventoryResponse getInventoryDetails(@PathVariable String skuCode) {
        return inventoryService.getInventoryDetails(skuCode);
    }

    // Приход/Списание через дельту
    @PostMapping("/adjust")
    public void adjustStock(@RequestBody InventoryRequest request) {
        inventoryService.adjustStock(request.getSkuCode(), request.getQuantity());
    }

    @PutMapping("/set-balance")
    public void setBalance(@RequestBody InventoryRequest request) {
        inventoryService.updateInventory(request);
    }

    // Удаление записи
    @DeleteMapping("/{skuCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventory(@PathVariable String skuCode) {
        inventoryService.deleteInventory(skuCode);
    }

    // Инициализация склада (вызывается при создании продукта)
    @PostMapping("/init/{skuCode}")
    @ResponseStatus(HttpStatus.CREATED)
    public void initStock(@PathVariable String skuCode) {
        inventoryService.initStock(skuCode);
    }

}