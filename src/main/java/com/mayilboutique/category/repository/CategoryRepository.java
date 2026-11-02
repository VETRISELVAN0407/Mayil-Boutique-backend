package com.mayilboutique.category.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mayilboutique.category.entity.Category;
import com.mayilboutique.core.enums.CategoryStatus;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByCreatedAtAsc();

    List<Category> findAllByStatusOrderByCreatedAtAsc(CategoryStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByImageFilename(String imageFilename);
}
