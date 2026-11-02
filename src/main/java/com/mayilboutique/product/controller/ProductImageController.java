package com.mayilboutique.product.controller;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.product.service.ProductImageStorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products/images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageStorageService imageStorage;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Path path = imageStorage.resolve(filename);
        // Filenames are unique per upload, so a given URL never changes content.
        return ResponseEntity.ok()
                .contentType(imageStorage.mediaTypeFor(filename))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(new FileSystemResource(path));
    }
}
