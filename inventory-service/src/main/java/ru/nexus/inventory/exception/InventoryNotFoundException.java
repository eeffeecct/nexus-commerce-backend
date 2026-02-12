package ru.nexus.inventory.exception;

public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String id) {
        super("Product with ID: " + id + " not found");
    }
}
