package com.mayilboutique.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mayilboutique.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByUserIdAndVariantId(Long userId, Long variantId);

    void deleteByUserIdAndVariantId(Long userId, Long variantId);
}
