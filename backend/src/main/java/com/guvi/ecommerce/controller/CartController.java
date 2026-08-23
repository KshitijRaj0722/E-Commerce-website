package com.guvi.ecommerce.controller;

import com.guvi.ecommerce.dto.CartItemResponse;
import com.guvi.ecommerce.dto.CartRequest;
import com.guvi.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<List<CartItemResponse>> getCart(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(cartService.getCart(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<CartItemResponse> addToCart(@AuthenticationPrincipal UserDetails user,
                                                      @Valid @RequestBody CartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(user.getUsername(), request));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<CartItemResponse> updateItem(@AuthenticationPrincipal UserDetails user,
                                                       @PathVariable Long itemId,
                                                       @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateCartItem(user.getUsername(), itemId, quantity));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal UserDetails user,
                                            @PathVariable Long itemId) {
        cartService.removeFromCart(user.getUsername(), itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserDetails user) {
        cartService.clearCart(user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
