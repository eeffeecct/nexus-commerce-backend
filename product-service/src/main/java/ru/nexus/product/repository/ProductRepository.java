package ru.nexus.product.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.nexus.product.entity.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
}
