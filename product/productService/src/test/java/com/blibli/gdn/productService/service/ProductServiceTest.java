import com.blibli.gdn.productService.dto.request.ProductRequest;
import com.blibli.gdn.productService.dto.request.VariantRequest;
import com.blibli.gdn.productService.dto.response.ProductResponse;
import com.blibli.gdn.productService.mapper.ProductMapper;
import com.blibli.gdn.productService.model.Product;
import com.blibli.gdn.productService.model.Variant;
import com.blibli.gdn.productService.repository.ProductRepository;
import com.blibli.gdn.productService.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Spy
    private ProductMapper productMapper = new ProductMapper();

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id("1")
                .productId("P001")
                .name("Test Product")
                .category("Electronics")
                .variants(java.util.Collections.singletonList(
                        Variant.builder()
                                .sku("P001-BLACK-001")
                                .price(100.0)
                                .stock(10)
                                .color("Black")
                                .size("M")
                                .build()
                ))
                .build();
                
        productRequest = ProductRequest.builder()
                .productId("P001")
                .name("Test Product")
                .category("Electronics")
                .variants(java.util.Collections.singletonList(
                        VariantRequest.builder()
                                .sku("P001-BLACK-001")
                                .price(100.0)
                                .stock(10)
                                .color("Black")
                                .size("M")
                                .build()
                ))
                .build();
    }

    @Test
    void createProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse created = productService.createProduct(productRequest);

        assertNotNull(created);
        assertEquals("Test Product", created.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void getProduct() {
        when(productRepository.findByProductId("P001")).thenReturn(Optional.of(product));

        ProductResponse found = productService.getProduct("P001");

        assertNotNull(found);
        assertEquals("1", found.getId());
    }

    @Test
    void getProduct_NotFound() {
        when(productRepository.findByProductId("P001")).thenReturn(Optional.empty());

        assertThrows(com.blibli.gdn.productService.exception.ProductNotFoundException.class, () -> productService.getProduct("P001"));
    }

    @Test
    void searchProducts() {
        Page<Product> page = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findByNameContainingIgnoreCase(anyString(), any(Pageable.class))).thenReturn(page);

        Page<ProductResponse> result = productService.searchProducts("Test", null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
