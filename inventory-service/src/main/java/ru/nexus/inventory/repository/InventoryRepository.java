package ru.nexus.inventory.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.nexus.inventory.entity.Inventory;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Inventory> inventoryRowMapper = (rs, rowNum) -> Inventory.builder()
            .id(rs.getLong("id"))
            .skuCode(rs.getString("sku_code"))
            .quantity(rs.getInt("quantity"))
            .version(rs.getInt("version"))
            .build();

    public Optional<Inventory> findBySkuCode(String skuCode) {
        String sql = "SELECT * FROM t_inventory WHERE sku_code = ?";
        try {
            Inventory inventory = jdbcTemplate.queryForObject(sql, inventoryRowMapper, skuCode);
            return Optional.ofNullable(inventory);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Inventory> findBySkuCodeAndQuantityGreaterThanEqual(String skuCode, Integer quantity) {
        // Implementing pessimistic lock using SELECT ... FOR UPDATE
        String sql = "SELECT * FROM t_inventory WHERE sku_code = ? AND quantity >= ? FOR UPDATE";
        try {
            Inventory inventory = jdbcTemplate.queryForObject(sql, inventoryRowMapper, skuCode, quantity);
            return Optional.ofNullable(inventory);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsBySkuCode(String skuCode) {
        String sql = "SELECT count(*) FROM t_inventory WHERE sku_code = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, skuCode);
        return count != null && count > 0;
    }

    public void save(Inventory inventory) {
        if (inventory.getId() == null) {
            String sql = "INSERT INTO t_inventory (sku_code, quantity, version) VALUES (?, ?, 0)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setString(1, inventory.getSkuCode());
                ps.setInt(2, inventory.getQuantity());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                inventory.setId(key.longValue());
                inventory.setVersion(0);
            }
        } else {
            String sql = "UPDATE t_inventory SET sku_code = ?, quantity = ?, version = version + 1 " +
                         " WHERE id = ? AND version = ?";
            int rowsAffected = jdbcTemplate.update(sql,
                    inventory.getSkuCode(),
                    inventory.getQuantity(),
                    inventory.getId(),
                    inventory.getVersion());

            if (rowsAffected == 0) {
                throw new RuntimeException("Optimistic lock failed: Record was updated by another user");
            }

            inventory.setVersion(inventory.getVersion() + 1);
        }
    }
}