package ru.nexus.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.nexus.product.dto.ProductRequest;
import ru.nexus.product.dto.ProductResponse;
import ru.nexus.product.entity.Product;
import ru.nexus.product.exception.ProductNotFoundException;
import ru.nexus.product.mapper.ProductMapper;
import ru.nexus.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repository;
    private final ProductMapper mapper;

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Fetching all products with pagination: {}", pageable);
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Cacheable(value = "products", key="#id")
    public ProductResponse getProductById(String id) {
        log.info("Fetching product by ID: {} (Cache miss if you see this)", id);
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return mapper.toResponse(product);
    }

    public ProductResponse createProduct(ProductRequest productRequest) {
        log.info("Creating new product with title: {}", productRequest.getTitle());
        Product product = mapper.toEntity(productRequest);
        Product savedProduct = repository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());
        return mapper.toResponse(savedProduct);
    }

    @CachePut(value = "products", key="#id")
    public ProductResponse updateProduct(String id, ProductRequest productRequest) {
        log.info("Updating product with ID: {}", id);
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        mapper.updateEntity(productRequest, product);

        Product updatedProduct = repository.save(product);
        log.info("Product updated successfully: {}", id);
        return mapper.toResponse(updatedProduct);
    }

    @CacheEvict(value = "products", key="#id")
    public void deleteProduct(String id) {
        log.info("Deleting product with ID: {}", id);
        if (!repository.existsById(id)) {
            log.warn("Attempt to delete non-existent product with ID: {}", id);
            throw new ProductNotFoundException(id);
        }
        repository.deleteById(id);
        log.info("Product deleted successfully: {}", id);
    }
}
