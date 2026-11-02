package com.mayilboutique.product.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.core.exception.GlobalExceptionHandler;
import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.product.controller.PublicProductController;
import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.product.service.ProductService;

class PublicProductControllerTest {

    private ProductService productService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productService = org.mockito.Mockito.mock(ProductService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicProductController(productService, org.mockito.Mockito.mock(com.mayilboutique.product.service.ProductVariantService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getPublishedProductsReturnsProductList() throws Exception {
        ProductResponse response = new ProductResponse(1L, "Summer Edit", "summer-edit", "Summer collection", 2L, null, null,
                "SKU-1", ProductStatus.PUBLISHED, 0, null, null);
        given(productService.getPublishedProducts(null, null)).willReturn(List.of(response));

        mockMvc.perform(get("/api/products/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].slug").value("summer-edit"));
    }

    @Test
    void getPublishedProductBySlugReturnsProduct() throws Exception {
        ProductResponse response = new ProductResponse(1L, "Summer Edit", "summer-edit", "Summer collection", 2L, null, null,
                "SKU-1", ProductStatus.PUBLISHED, 0, null, null);
        given(productService.getPublishedProductBySlug("summer-edit")).willReturn(response);

        mockMvc.perform(get("/api/products/public/summer-edit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.slug").value("summer-edit"));
    }

    @Test
    void getPublishedProductBySlugReturnsNotFoundWhenMissing() throws Exception {
        willThrow(new BusinessException(HttpStatus.NOT_FOUND, "Product not found"))
                .given(productService).getPublishedProductBySlug("missing-slug");

        mockMvc.perform(get("/api/products/public/missing-slug"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }
}
