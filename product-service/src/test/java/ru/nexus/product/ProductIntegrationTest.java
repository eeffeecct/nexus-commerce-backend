package ru.nexus.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.nexus.common.test.ContainerFactory;
import ru.nexus.product.dto.ProductRequest;
import ru.nexus.product.dto.ProductResponse;
import ru.nexus.product.entity.Product;
import ru.nexus.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = ContainerFactory.mongo();

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = ContainerFactory.redis();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        try {
            Objects.requireNonNull(redisTemplate.keys("products::*")).forEach(key -> redisTemplate.delete(key));
        } catch (Exception e) {
            // Redis might be empty or connection issue, ignore for now in test setup
        }
    }

    @Test
    @DisplayName("Should create product successfully")
    void createProduct() throws Exception {
        ProductRequest productRequest = ProductRequest.builder()
                .title("iPhone 15")
                .price(BigDecimal.valueOf(1200))
                .quantity(10)
                .category("Electronics")
                .build();

        String productJson = objectMapper.writeValueAsString(productRequest);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("iPhone 15"))
                .andExpect(jsonPath("$.price").value(1200));

        var storedProducts = productRepository.findAll();
        assertThat(storedProducts).hasSize(1);
        assertThat(storedProducts.getFirst().getTitle()).isEqualTo("iPhone 15");
    }

    @Test
    @DisplayName("Should return 400 when validation fails")
    void createInvalid() throws Exception {
        ProductRequest invalidRequest = ProductRequest.builder()
                .title("Bad Product")
                .price(BigDecimal.valueOf(-500))
                // .quantity(5) // Missing -> @NotNull
                // .category("Test") // Missing -> @NotBlank
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.quantity").exists())
                .andExpect(jsonPath("$.errors.category").exists());
        
        assertThat(productRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return 404 when product not found")
    void notFound() throws Exception {
        String randomId = "nonExistentId";

        mockMvc.perform(get("/api/v1/products/{id}", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should cache product after get")
    void cacheOnGet() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .title("Cached Item")
                .price(BigDecimal.TEN)
                .quantity(1)
                .category("Test")
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ProductResponse response = objectMapper.readValue(responseJson, ProductResponse.class);
        String productId = response.getId();

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk());

        Object cachedValue = redisTemplate.opsForValue().get("products::" + productId);
        assertThat(cachedValue).isNotNull();
    }

    @Test
    @DisplayName("Should update product and update cache")
    void updateProduct() throws Exception {
        Product savedProduct = productRepository.save(Product.builder()
                .title("Old Title")
                .price(BigDecimal.TEN)
                .quantity(1)
                .category("Old Cat")
                .build());

        ProductRequest updateRequest = ProductRequest.builder()
                .title("New Title")
                .price(BigDecimal.valueOf(20))
                .quantity(2)
                .category("New Cat")
                .build();

        mockMvc.perform(put("/api/v1/products/{id}", savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));

        Object cachedValue = redisTemplate.opsForValue().get("products::" + savedProduct.getId());
        assertThat(cachedValue).isNotNull();
    }
    
    @Test
    @DisplayName("Should delete product and evict cache")
    void deleteProduct() throws Exception {
        Product savedProduct = productRepository.save(Product.builder()
                .title("To Delete").price(BigDecimal.TEN).quantity(1).category("Del").build());
        
        mockMvc.perform(get("/api/v1/products/{id}", savedProduct.getId()));
        assertThat(redisTemplate.hasKey("products::" + savedProduct.getId())).isTrue();

        mockMvc.perform(delete("/api/v1/products/{id}", savedProduct.getId()))
                .andExpect(status().isNoContent()); // 204

        assertThat(productRepository.existsById(savedProduct.getId())).isFalse();
        assertThat(redisTemplate.hasKey("products::" + savedProduct.getId())).isFalse();
    }
    
    @Test
    @DisplayName("Should get all products")
    void getAll() throws Exception {
        productRepository.save(Product.builder().title("P1").price(BigDecimal.TEN).quantity(1).category("C").build());
        productRepository.save(Product.builder().title("P2").price(BigDecimal.TEN).quantity(1).category("C").build());

        mockMvc.perform(get("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }
}
