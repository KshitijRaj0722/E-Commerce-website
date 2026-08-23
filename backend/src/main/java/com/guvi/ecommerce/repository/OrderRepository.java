package com.guvi.ecommerce.repository;

import com.guvi.ecommerce.entity.Order;
import com.guvi.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
}
