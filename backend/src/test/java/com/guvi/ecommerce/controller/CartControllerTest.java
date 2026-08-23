package com.guvi.ecommerce.controller;

import com.guvi.ecommerce.config.JwtUtil;
import com.guvi.ecommerce.dto.CartItemResponse;
import com.guvi.ecommerce.entity.Product;
import com.guvi.ecommerce.exception.BadRequestException;
import com.guvi.ecommerce.service.CartService;
import com.guvi.ecommerce.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CartService cartService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private CartItemResponse sampleItem() {
        Product product = Product.builder().id(1L).name("Lamp")
                .price(BigDecimal.valueOf(500)).stock(4).build();
        return new CartItemResponse(5L, product, 2, BigDecimal.valueOf(1000));
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
    void getCart_returnsItemsWithSubtotalAndNoUserPayload() throws Exception {
        when(cartService.getCart("buyer@test.com")).thenReturn(List.of(sampleItem()));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].subtotal").value(1000))
                .andExpect(jsonPath("$[0].product.name").value("Lamp"))
                // the cart response must never carry the owning user (and their password hash)
                .andExpect(jsonPath("$[0].user").doesNotExist());
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
    void addToCart_beyondStock_returns400WithMessage() throws Exception {
        when(cartService.addToCart(eq("buyer@test.com"), any()))
                .thenThrow(new BadRequestException("Only 4 unit(s) of Lamp are in stock"));

        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only 4 unit(s) of Lamp are in stock"));
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
    void addToCart_invalidQuantity_failsValidation() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("quantity")));

        verify(cartService, never()).addToCart(any(), any());
    }

    @Test
    @WithMockUser(username = "buyer@test.com")
    void removeItem_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/cart/5"))
                .andExpect(status().isNoContent());

        verify(cartService).removeFromCart("buyer@test.com", 5L);
    }
}
