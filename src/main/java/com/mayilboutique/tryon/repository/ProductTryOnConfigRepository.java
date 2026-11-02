package com.mayilboutique.tryon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mayilboutique.tryon.entity.ProductTryOnConfig;

public interface ProductTryOnConfigRepository extends JpaRepository<ProductTryOnConfig, Long> {

    List<ProductTryOnConfig> findByEnabledTrueAndAiReadyTrueAndPrimaryImageIdIsNotNull();
}
