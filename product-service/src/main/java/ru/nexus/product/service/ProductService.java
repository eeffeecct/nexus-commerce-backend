package ru.nexus.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nexus.product.dto.ProductRequest;
import ru.nexus.product.dto.ProductResponse;
import ru.nexus.product.entity.Product;
import ru.nexus.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repository;

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public ProductResponse getProductById(String id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    public ProductResponse createProduct(ProductRequest productRequest) {
        if (productRequest == null) {
            throw new IllegalArgumentException("ProductRequest cannot be null");
            //TODO: Custom Exceptions + Exception Handler
        }
        Product product = Product.builder()
                .title(productRequest.getTitle())
                .price(productRequest.getPrice())
                .quantity(productRequest.getQuantity())
                .category(productRequest.getCategory())
                .attributes(productRequest.getAttributes())
                .build();
        Product savedProduct = repository.save(product);
        return mapToResponse(savedProduct);
    }

    public ProductResponse updateProduct(String id, ProductRequest productRequest) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setTitle(productRequest.getTitle());
        product.setPrice(productRequest.getPrice());
        product.setQuantity(productRequest.getQuantity());
        product.setCategory(productRequest.getCategory());
        product.setAttributes(productRequest.getAttributes());

        Product updatedProduct = repository.save(product);
        return mapToResponse(updatedProduct);
    }

    public void deleteProduct(String id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Cannot delete. Product not found with id: " + id);
        }
        repository.deleteById(id);
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getQuantity(),
                product.getCategory(),
                product.getAttributes()
        );
    }
}
