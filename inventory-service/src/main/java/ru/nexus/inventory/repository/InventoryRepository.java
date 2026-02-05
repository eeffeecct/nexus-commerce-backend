package ru.nexus.inventory.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.nexus.inventory.entity.Inventory;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findBySkuCode(String skuCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findBySkuCodeAndQuantityGreaterThanEqual(String skuCode, Integer quantity);

    boolean existsBySkuCode(String skuCode);
}
