package ru.nexus.inventory.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.nexus.common.event.ProductCreatedEvent;
import ru.nexus.inventory.config.RabbitMQConfig;
import ru.nexus.inventory.service.InventoryService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCreatedListener {

    private final InventoryService inventoryService;

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_CREATED_QUEUE)
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("Received ProductCreatedEvent for SKU: {}", event.getSkuCode());
        try {
            inventoryService.initStock(event.getSkuCode());
            log.info("Successfully initialized stock for SKU: {}", event.getSkuCode());
        } catch (Exception e) {
            log.error("Error processing ProductCreatedEvent for SKU: {}. Error: {}", event.getSkuCode(), e.getMessage());
        }
    }
}
