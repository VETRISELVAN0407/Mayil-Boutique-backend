package com.mayilboutique.product.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.core.exception.GlobalExceptionHandler;
import com.mayilboutique.product.dto.request.ProductRequest;
import com.mayilboutique.product.dto.response.ProductImageResponse;
import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.product.service.ProductImageService;
import com.mayilboutique.product.service.ProductImageStorageService;
import com.mayilboutique.product.service.ProductService;

class ProductImageControllerTest {

    @TempDir
    Path dir;
    private ProductImageStorageService storage;
    private ProductService productService;
    private ProductImageService imageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        storage = org.mockito.Mockito.mock(ProductImageStorageService.class);
        productService = org.mockito.Mockito.mock(ProductService.class);
        imageService = org.mockito.Mockito.mock(ProductImageService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductImageController(storage),
                        new AdminProductController(productService), new AdminProductImageController(imageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void servesImageWithContentType() throws Exception {
        Path file = dir.resolve("a.png");
        Files.write(file, new byte[] {1, 2, 3});
        String name = "0123456789abcdef0123456789abcdef.png";
        given(storage.resolve(name)).willReturn(file);
        given(storage.mediaTypeFor(name)).willReturn(MediaType.IMAGE_PNG);

        mockMvc.perform(get("/api/products/images/" + name))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    void returnsNotFoundForMissingImage() throws Exception {
        given(storage.resolve("nope.png")).willThrow(new BusinessException(HttpStatus.NOT_FOUND, "Image not found"));
        mockMvc.perform(get("/api/products/images/nope.png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Image not found"));
    }

    @Test
    void multipartCreateAcceptsProductAndImage() throws Exception {
        MockMultipartFile product = new MockMultipartFile("product", "", "application/json",
                "{\"name\":\"K\",\"slug\":\"k\",\"price\":10,\"sku\":\"S\"}".getBytes());
        MockMultipartFile image = new MockMultipartFile("images", "a.png", "image/png", new byte[] {1});
        given(productService.createProduct(org.mockito.ArgumentMatchers.any(ProductRequest.class),
                org.mockito.ArgumentMatchers.any()))
                .willReturn(new ProductResponse(1L, "K", "k", null, null, null, null, "S", ProductStatus.DRAFT, 0,
                        "x.png", null, null));

        mockMvc.perform(multipart("/api/admin/products").file(product).file(image))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageFilename").value("x.png"));
    }

    @Test
    void multipartCreateAcceptsSeveralImages() throws Exception {
        MockMultipartFile product = new MockMultipartFile("product", "", "application/json",
                "{\"name\":\"K\",\"slug\":\"k\",\"price\":10,\"sku\":\"S\"}".getBytes());
        given(productService.createProduct(org.mockito.ArgumentMatchers.any(ProductRequest.class),
                org.mockito.ArgumentMatchers.argThat(list -> list != null && list.size() == 2)))
                .willReturn(withImages());

        mockMvc.perform(multipart("/api/admin/products").file(product)
                        .file(new MockMultipartFile("images", "a.png", "image/png", new byte[] {1}))
                        .file(new MockMultipartFile("images", "b.png", "image/png", new byte[] {2})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images.length()").value(2));
    }

    @Test
    void addImagesReturnsTheUpdatedProduct() throws Exception {
        given(imageService.add(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyList()))
                .willReturn(withImages());

        mockMvc.perform(multipart("/api/admin/products/1/images")
                        .file(new MockMultipartFile("images", "a.png", "image/png", new byte[] {1})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageFilename").value("x.png"))
                .andExpect(jsonPath("$.images[1].id").value(11));
    }

    @Test
    void addImagesWithoutAnImagesPartIsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/products/1/images"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addImagesPropagatesTheLimitError() throws Exception {
        given(imageService.add(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyList()))
                .willThrow(new BusinessException(HttpStatus.BAD_REQUEST, "A product can have at most 8 images"));

        mockMvc.perform(multipart("/api/admin/products/1/images")
                        .file(new MockMultipartFile("images", "a.png", "image/png", new byte[] {1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A product can have at most 8 images"));
    }

    @Test
    void reorderPassesTheIdsInOrder() throws Exception {
        given(imageService.reorder(1L, java.util.List.of(11L, 10L))).willReturn(withImages());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/products/1/images/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageIds\":[11,10]}"))
                .andExpect(status().isOk());
    }

    @Test
    void reorderWithoutIdsIsBadRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/products/1/images/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturnsTheUpdatedProduct() throws Exception {
        given(imageService.delete(1L, 10L)).willReturn(withImages());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/products/1/images/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(2));
    }

    private ProductResponse withImages() {
        ProductResponse response = new ProductResponse(1L, "K", "k", null, null, null, null, "S", ProductStatus.DRAFT, 0,
                "x.png", null, null);
        response.setImages(java.util.List.of(new ProductImageResponse(10L, "x.png"), new ProductImageResponse(11L, "y.png")));
        return response;
    }
}
