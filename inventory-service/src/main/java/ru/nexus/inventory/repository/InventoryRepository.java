package ru.nexus.inventory.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.nexus.inventory.entity.Inventory;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final RowMapper<Inventory> inventoryRowMapper = (rs, rowNum) -> Inventory.builder()
            .id(rs.getLong("id"))
            .skuCode(rs.getString("sku_code"))
            .quantity(rs.getInt("quantity"))
            .version(rs.getInt("version"))
            .build();


    public Optional<Inventory> findBySkuCode(String skuCode) {
        String sql = "SELECT * FROM t_inventory WHERE sku_code = ? LIMIT 1";
        return jdbcTemplate.query(sql, inventoryRowMapper, skuCode).stream().findFirst();
    }

    public boolean existsBySkuCode(String skuCode) {
        String sql = "SELECT count(*) FROM t_inventory WHERE sku_code = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, skuCode);
        return count != null && count > 0;
    }

    public List<Inventory> findAllBySkuCodes(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return Collections.emptyList();
        }

        String sql = "SELECT id, sku_code, quantity, version FROM t_inventory WHERE sku_code IN (:skuCodes)";

        var params = new MapSqlParameterSource("skuCodes", skuCodes);

        return namedParameterJdbcTemplate.query(sql, params, inventoryRowMapper);
    }

    public void saveInventory(Inventory inventory) {
        if (inventory.getId() == null) {
            insert(inventory);
        } else {
            update(inventory);
        }
    }

    private void insert(Inventory inventory) {
        String sql = "INSERT INTO t_inventory (sku_code, quantity, version) VALUES (?, ?, ?)";
        
        inventory.setVersion(0);

        try {
            jdbcTemplate.update(sql,
                    inventory.getSkuCode(),
                    inventory.getQuantity(),
                    inventory.getVersion());
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert inventory for SKU: " + inventory.getSkuCode() + ". SQL: " + sql, e);
        }
    }

    private void update(Inventory inventory) {
        String sql = "UPDATE t_inventory SET sku_code = ?, quantity = ?, version = version + 1 WHERE id = ? AND version = ?";

        int rowsAffected = jdbcTemplate.update(sql,
                inventory.getSkuCode(),
                inventory.getQuantity(),
                inventory.getId(),
                inventory.getVersion());

        if (rowsAffected == 0) {
            throw new OptimisticLockingFailureException("Inventory modified by another transaction");
        }

        inventory.setVersion(inventory.getVersion() + 1);
    }

    public int updateQuantity(String skuCode, Integer delta) {
        String sql = "UPDATE t_inventory SET quantity = quantity + ? WHERE sku_code = ? AND (quantity + ?) >= 0";

        return jdbcTemplate.update(sql, delta, skuCode, delta);
    }

    public int deleteBySkuCode(String skuCode) {
        String sql = "DELETE FROM t_inventory WHERE sku_code = ?";
        return jdbcTemplate.update(sql, skuCode);
    }
}
