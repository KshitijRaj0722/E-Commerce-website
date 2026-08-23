package com.guvi.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guvi.ecommerce.config.JwtUtil;
import com.guvi.ecommerce.dto.ProductRequest;
import com.guvi.ecommerce.entity.Product;
import com.guvi.ecommerce.exception.ResourceNotFoundException;
import com.guvi.ecommerce.service.ProductService;
import com.guvi.ecommerce.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ProductService productService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private Product sample() {
        return Product.builder().id(1L).name("Wireless Mouse").description("Ergonomic")
                .price(BigDecimal.valueOf(799)).stock(10).category("electronics").build();
    }

    @Test
    void getAll_returnsProductList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Wireless Mouse"))
                .andExpect(jsonPath("$[0].price").value(799));
    }

    @Test
    void getById_unknownId_returns404WithMessage() throws Exception {
        when(productService.getProduct(99L)).thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void search_passesQueryToService() throws Exception {
        when(productService.searchProducts("mouse")).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/products/search").param("query", "mouse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(productService).searchProducts("mouse");
    }

    @Test
    void create_validPayload_returnsCreatedProduct() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Keyboard");
        request.setPrice(BigDecimal.valueOf(1499));
        request.setStock(5);
        when(productService.createProduct(any())).thenReturn(sample());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_missingName_returns400FromValidation() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setPrice(BigDecimal.valueOf(10));
        request.setStock(1);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));

        verify(productService, never()).createProduct(any());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(eq(1L));
    }
}
