package com.mayilboutique.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mayilboutique.review.entity.ProductReview;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductId(Long productId);

    Optional<ProductReview> findByUserIdAndProductId(Long userId, Long productId);
}
