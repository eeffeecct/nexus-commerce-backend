package ru.nexus.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String id) {
        super("Stock Not enough for ID " + id);
    }
}
