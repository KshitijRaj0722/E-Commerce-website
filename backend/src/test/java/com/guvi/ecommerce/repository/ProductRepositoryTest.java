package com.guvi.ecommerce.repository;

import com.guvi.ecommerce.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void seed() {
        productRepository.save(Product.builder().name("Wireless Mouse")
                .price(BigDecimal.valueOf(799)).stock(10).category("electronics").build());
        productRepository.save(Product.builder().name("Mouse Pad")
                .price(BigDecimal.valueOf(299)).stock(5).category("accessories").build());
        productRepository.save(Product.builder().name("Keyboard")
                .price(BigDecimal.valueOf(1499)).stock(3).category("electronics").build());
    }

    @Test
    void findByNameContainingIgnoreCase_matchesPartialAndIsCaseInsensitive() {
        List<Product> results = productRepository.findByNameContainingIgnoreCase("mOuSe");

        assertThat(results).hasSize(2)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Wireless Mouse", "Mouse Pad");
    }

    @Test
    void findByNameContainingIgnoreCase_noMatch_returnsEmpty() {
        assertThat(productRepository.findByNameContainingIgnoreCase("monitor")).isEmpty();
    }

    @Test
    void findByCategory_filtersCorrectly() {
        assertThat(productRepository.findByCategory("electronics")).hasSize(2);
        assertThat(productRepository.findByCategory("accessories")).hasSize(1);
    }
}
