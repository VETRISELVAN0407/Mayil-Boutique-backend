package com.mayilboutique.product.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.mayilboutique.category.repository.CategoryRepository;
import com.mayilboutique.order.repository.OrderItemRepository;
import com.mayilboutique.product.repository.ProductImageRepository;

/**
 * Keeps image files in step with the database. A new file is removed if the surrounding transaction rolls back,
 * and a file is removed after commit only once no product image and no order line still refers to it
 * (order lines keep the picture of what was bought).
 */
@Component
public class ProductImageFiles {

    private final ProductImageStorageService storage;
    private final ProductImageRepository productImageRepository;
    private final OrderItemRepository orderItemRepository;
    private final CategoryRepository categoryRepository;

    public ProductImageFiles(ProductImageStorageService storage, ProductImageRepository productImageRepository,
                             OrderItemRepository orderItemRepository, CategoryRepository categoryRepository) {
        this.storage = storage;
        this.productImageRepository = productImageRepository;
        this.orderItemRepository = orderItemRepository;
        this.categoryRepository = categoryRepository;
    }

    /** Stores the upload and returns its generated filename; the file is dropped again if the transaction fails. */
    public String store(MultipartFile file) {
        String filename = storage.store(file);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        storage.delete(filename);
                    }
                }
            });
        }
        return filename;
    }

    /**
     * Call inside the transaction, after the database rows for {@code filename} were removed. The file is deleted
     * after commit unless something still references it.
     */
    public void discardIfUnused(String filename) {
        if (filename == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        if (productImageRepository.existsByFilename(filename) || orderItemRepository.existsByImageFilename(filename)
                || categoryRepository.existsByImageFilename(filename)) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.delete(filename);
            }
        });
    }
}
