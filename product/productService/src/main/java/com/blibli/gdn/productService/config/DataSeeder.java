package com.blibli.gdn.productService.config;

import com.blibli.gdn.productService.model.Product;
import com.blibli.gdn.productService.model.Variant;
import com.blibli.gdn.productService.repository.ProductRepository;
import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            Faker faker = new Faker();
            List<Product> batch = new ArrayList<>();
            for (int i = 0; i < 50000; i++) {
                String productId = "SHT-60001-" + faker.number().randomNumber(6, true);
                String productName = faker.commerce().productName();

                int variantCount = faker.number().numberBetween(2, 4);
                List<Variant> variants = new ArrayList<>();
                for (int v = 0; v < variantCount; v++) {
                    String sku = productId + "-" + String.format("%05d", v);
                    variants.add(Variant.builder()
                            .sku(sku)
                            .size(faker.options().option("S", "M", "L", "XL"))
                            .color(faker.color().name())
                            .price(Double.parseDouble(faker.commerce().price().replace(",", ".")))
                            .stock(faker.number().numberBetween(1, 100))
                            .build());
                }

                Product product = Product.builder()
                        .productId(productId)
                        .name(productName)
                        .description(faker.lorem().sentence())
                        .category(faker.commerce().department())
                        .brand(faker.company().name())
                        .tags(Collections.singletonList(faker.commerce().material()))
                        .variants(variants)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

                batch.add(product);

                if (i % 1000 == 0) {
                    productRepository.saveAll(batch);
                    batch.clear();
                    System.out.println("Seeded " + i + " products...");
                }
            }
            if (!batch.isEmpty()) {
                productRepository.saveAll(batch);
            }
            System.out.println("Seeding Complete! 50,000 products added.");
        }
    }
}
