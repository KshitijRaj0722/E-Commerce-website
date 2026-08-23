package com.guvi.ecommerce;

import com.guvi.ecommerce.dto.ProductRequest;
import com.guvi.ecommerce.entity.Product;
import com.guvi.ecommerce.repository.ProductRepository;
import com.guvi.ecommerce.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getAllProducts_returnsList() {
        Product p = Product.builder().id(1L).name("Test").price(BigDecimal.TEN).stock(5).build();
        when(productRepository.findAll()).thenReturn(List.of(p));

        List<Product> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test");
    }

    @Test
    void createProduct_savesAndReturns() {
        ProductRequest req = new ProductRequest();
        req.setName("Widget");
        req.setPrice(BigDecimal.valueOf(29.99));
        req.setStock(10);

        Product saved = Product.builder().id(1L).name("Widget").price(req.getPrice()).stock(10).build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        Product result = productService.createProduct(req);

        assertThat(result.getId()).isEqualTo(1L);
        verify(productRepository).save(any());
    }

    @Test
    void getProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> productService.getProduct(99L));
    }
}
