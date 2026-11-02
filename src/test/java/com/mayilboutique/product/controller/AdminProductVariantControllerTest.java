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

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.core.exception.GlobalExceptionHandler;
import com.mayilboutique.product.controller.AdminProductVariantController;
import com.mayilboutique.product.dto.request.CreateProductVariantRequest;
import com.mayilboutique.product.dto.request.UpdateProductVariantRequest;
import com.mayilboutique.product.dto.request.UpdateProductVariantStockRequest;
import com.mayilboutique.product.dto.response.ProductVariantResponse;
import com.mayilboutique.product.service.ProductVariantService;

class AdminProductVariantControllerTest {

    private ProductVariantService productVariantService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productVariantService = org.mockito.Mockito.mock(ProductVariantService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminProductVariantController(productVariantService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createVariantReturnsCreatedVariant() throws Exception {
        ProductVariantResponse response = new ProductVariantResponse(1L, 2L, "SKU-2", "M", "Blue", 0, null, null);
        given(productVariantService.createVariant(eq(2L), any(CreateProductVariantRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/admin/products/2/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-2\",\"size\":\"M\",\"colour\":\"Blue\",\"stockQuantity\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.sku").value("SKU-2"))
                .andExpect(jsonPath("$.stockQuantity").value(0));
    }

    @Test
    void createVariantAcceptsAndReturnsTheSizeNumber() throws Exception {
        ProductVariantResponse response = new ProductVariantResponse(1L, 2L, "SKU-2", "M", "32", "Blue", 0, null, null);
        given(productVariantService.createVariant(eq(2L), any(CreateProductVariantRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/admin/products/2/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-2\",\"size\":\"M\",\"sizeNumber\":\"32\",\"colour\":\"Blue\",\"stockQuantity\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sizeNumber").value("32"));
    }

    @Test
    void createVariantRejectsAnInvalidSizeNumber() throws Exception {
        mockMvc.perform(post("/api/admin/products/2/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-2\",\"size\":\"M\",\"sizeNumber\":\"<b>\",\"stockQuantity\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/products/2/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-2\",\"size\":\"M\",\"sizeNumber\":\"12345678901\",\"stockQuantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createVariantReturnsBadRequestWhenSkuIsMissing() throws Exception {
        mockMvc.perform(post("/api/admin/products/2/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"size\":\"M\",\"colour\":\"Blue\",\"stockQuantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createVariantReturnsConflictWhenSkuExists() throws Exception {
        willThrow(new BusinessException(HttpStatus.CONFLICT, "Product variant SKU already exists"))
                .given(productVariantService).createVariant(eq(2L), any(CreateProductVariantRequest.class));

        mockMvc.perform(post("/api/admin/products/2/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-2\",\"size\":\"M\",\"colour\":\"Blue\",\"stockQuantity\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Product variant SKU already exists"));
    }

    @Test
    void getVariantsReturnsNotFoundWhenProductMissing() throws Exception {
        given(productVariantService.getVariants(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));

        mockMvc.perform(get("/api/admin/products/999/variants"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void updateVariantReturnsUpdatedVariant() throws Exception {
        ProductVariantResponse response = new ProductVariantResponse(1L, 2L, "SKU-2", "L", "Black", 5, null, null);
        given(productVariantService.updateVariant(eq(2L), eq(1L), any(UpdateProductVariantRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/admin/products/2/variants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-2\",\"size\":\"L\",\"colour\":\"Black\",\"stockQuantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-2"))
                .andExpect(jsonPath("$.stockQuantity").value(5));
    }

    @Test
    void updateVariantReturnsConflictWhenSkuAlreadyExists() throws Exception {
        willThrow(new BusinessException(HttpStatus.CONFLICT, "Product variant SKU already exists"))
                .given(productVariantService).updateVariant(eq(2L), eq(1L), any(UpdateProductVariantRequest.class));

        mockMvc.perform(put("/api/admin/products/2/variants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-2\",\"size\":\"L\",\"colour\":\"Black\",\"stockQuantity\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Product variant SKU already exists"));
    }

    @Test
    void adjustStockReturnsUpdatedVariant() throws Exception {
        ProductVariantResponse response = new ProductVariantResponse(1L, 2L, "SKU-2", "M", "Blue", 12, null, null);
        given(productVariantService.adjustStock(eq(2L), eq(1L), any(UpdateProductVariantStockRequest.class))).willReturn(response);

        mockMvc.perform(patch("/api/admin/products/2/variants/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockQuantity\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stockQuantity").value(12));
    }

    @Test
    void adjustStockReturnsBadRequestWhenStockIsNegative() throws Exception {
        willThrow(new BusinessException(HttpStatus.BAD_REQUEST, "Stock quantity cannot be negative"))
                .given(productVariantService).adjustStock(eq(2L), eq(1L), any(UpdateProductVariantStockRequest.class));

        mockMvc.perform(patch("/api/admin/products/2/variants/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockQuantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("stockQuantity: Stock quantity cannot be negative"));
    }

    @Test
    void deleteVariantReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/products/2/variants/1"))
                .andExpect(status().isNoContent());
    }
}
