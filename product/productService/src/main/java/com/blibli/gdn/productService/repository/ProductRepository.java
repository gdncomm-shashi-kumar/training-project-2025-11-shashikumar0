package com.blibli.gdn.productService.repository;

import com.blibli.gdn.productService.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Page<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    java.util.Optional<Product> findByVariantsSku(String sku);
    
    java.util.Optional<Product> findByProductId(String productId);
}
