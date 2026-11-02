package com.mayilboutique.category.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.mayilboutique.category.controller.AdminCategoryController;
import com.mayilboutique.category.dto.request.CategoryRequest;
import com.mayilboutique.category.dto.request.UpdateCategoryStatusRequest;
import com.mayilboutique.category.dto.response.CategoryResponse;
import com.mayilboutique.category.service.CategoryService;
import com.mayilboutique.core.enums.CategoryStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.core.exception.GlobalExceptionHandler;

class AdminCategoryControllerTest {

    private CategoryService categoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoryService = org.mockito.Mockito.mock(CategoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCategoryController(categoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createCategoryReturnsCreatedCategory() throws Exception {
        CategoryResponse response = new CategoryResponse(1L, "Summer Edit", "summer-edit", "Summer collection", "image-url", null,
                CategoryStatus.ACTIVE, null, null);
        given(categoryService.createCategory(any(CategoryRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Summer Edit\",\"slug\":\"summer-edit\",\"description\":\"Summer collection\",\"image\":\"image-url\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Summer Edit"))
                .andExpect(jsonPath("$.slug").value("summer-edit"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createCategoryReturnsBadRequestWhenNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"summer-edit\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategoryReturnsNotFoundWhenMissing() throws Exception {
        given(categoryService.getCategory(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "Category not found"));

        mockMvc.perform(get("/api/admin/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void updateCategoryStatusReturnsUpdatedCategory() throws Exception {
        CategoryResponse response = new CategoryResponse(1L, "Summer Edit", "summer-edit", "Summer collection", "image-url", null,
                CategoryStatus.INACTIVE, null, null);
        given(categoryService.updateStatus(1L, CategoryStatus.INACTIVE)).willReturn(response);

        mockMvc.perform(patch("/api/admin/categories/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deleteCategoryReturnsConflictWhenProductsUseCategory() throws Exception {
        willThrow(new BusinessException(HttpStatus.CONFLICT,
                "Category cannot be deleted because products are associated with it"))
                .given(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/admin/categories/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category cannot be deleted because products are associated with it"));
    }
}
