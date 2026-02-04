package ru.nexus.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import ru.nexus.product.dto.ProductRequest;
import ru.nexus.product.dto.ProductResponse;
import ru.nexus.product.entity.Product;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    Product toEntity(ProductRequest request);

    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);
}
