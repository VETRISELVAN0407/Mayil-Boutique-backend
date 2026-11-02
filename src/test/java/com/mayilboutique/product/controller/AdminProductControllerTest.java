package com.mayilboutique.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.core.exception.GlobalExceptionHandler;
import com.mayilboutique.product.controller.AdminProductController;
import com.mayilboutique.product.dto.request.ProductRequest;
import com.mayilboutique.product.dto.request.UpdateProductStatusRequest;
import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.product.service.ProductService;

class AdminProductControllerTest {

    private ProductService productService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productService = org.mockito.Mockito.mock(ProductService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createProductReturnsCreatedProduct() throws Exception {
        ProductResponse response = new ProductResponse(1L, "Summer Edit", "summer-edit", "Summer collection", 2L, null, null,
                "SKU-1", ProductStatus.DRAFT, 0, null, null);
        given(productService.createProduct(any(ProductRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Summer Edit\",\"slug\":\"summer-edit\",\"description\":\"Summer collection\",\"categoryId\":2,\"price\":999.00,\"salePrice\":799.00,\"sku\":\"SKU-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Summer Edit"))
                .andExpect(jsonPath("$.slug").value("summer-edit"))
                .andExpect(jsonPath("$.sku").value("SKU-1"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createProductReturnsBadRequestWhenNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"summer-edit\",\"price\":999.00,\"sku\":\"SKU-1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProductReturnsConflictWhenSlugExists() throws Exception {
        willThrow(new BusinessException(org.springframework.http.HttpStatus.CONFLICT, "Product slug already exists"))
                .given(productService).createProduct(any(ProductRequest.class));

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Summer Edit\",\"slug\":\"summer-edit\",\"price\":999.00,\"sku\":\"SKU-1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Product slug already exists"));
    }

    @Test
    void getProductReturnsNotFoundWhenMissing() throws Exception {
        given(productService.getProduct(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));

        mockMvc.perform(get("/api/admin/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void updateProductReturnsUpdatedProduct() throws Exception {
        ProductResponse response = new ProductResponse(1L, "Summer Edit", "summer-edit", "Summer collection", 2L, null, null,
                "SKU-1", ProductStatus.DRAFT, 0, null, null);
        given(productService.updateProduct(eq(1L), any(ProductRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/admin/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Summer Edit\",\"slug\":\"summer-edit\",\"description\":\"Summer collection\",\"categoryId\":2,\"price\":999.00,\"salePrice\":799.00,\"sku\":\"SKU-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Summer Edit"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void updateProductReturnsConflictWhenSlugAlreadyExists() throws Exception {
        willThrow(new BusinessException(HttpStatus.CONFLICT, "Product slug already exists"))
                .given(productService).updateProduct(eq(1L), any(ProductRequest.class));

        mockMvc.perform(put("/api/admin/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Summer Edit\",\"slug\":\"summer-edit\",\"price\":999.00,\"sku\":\"SKU-1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Product slug already exists"));
    }

    @Test
    void updateProductStatusReturnsUpdatedProduct() throws Exception {
        ProductResponse response = new ProductResponse(1L, "Summer Edit", "summer-edit", "Summer collection", 2L, null, null,
                "SKU-1", ProductStatus.PUBLISHED, 0, null, null);
        given(productService.updateStatus(1L, ProductStatus.PUBLISHED)).willReturn(response);

        mockMvc.perform(patch("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void deleteProductReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateProductStatusReturnsBadRequestWhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
