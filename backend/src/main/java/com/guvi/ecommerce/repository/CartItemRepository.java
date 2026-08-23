package com.guvi.ecommerce.repository;

import com.guvi.ecommerce.entity.CartItem;
import com.guvi.ecommerce.entity.User;
import com.guvi.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    void deleteByUser(User user);
}
