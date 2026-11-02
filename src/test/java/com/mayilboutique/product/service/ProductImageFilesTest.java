package com.mayilboutique.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.mayilboutique.category.repository.CategoryRepository;
import com.mayilboutique.order.repository.OrderItemRepository;
import com.mayilboutique.product.repository.ProductImageRepository;

class ProductImageFilesTest {

    private ProductImageStorageService storage;
    private ProductImageRepository images;
    private OrderItemRepository orderItems;
    private CategoryRepository categories;
    private ProductImageFiles files;
    private final MockMultipartFile upload = new MockMultipartFile("images", "a.png", "image/png", new byte[] {1});

    @BeforeEach
    void setUp() {
        storage = mock(ProductImageStorageService.class);
        images = mock(ProductImageRepository.class);
        orderItems = mock(OrderItemRepository.class);
        categories = mock(CategoryRepository.class);
        files = new ProductImageFiles(storage, images, orderItems, categories);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void complete(int status) {
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            if (status == TransactionSynchronization.STATUS_COMMITTED) {
                s.afterCommit();
            }
            s.afterCompletion(status);
        }
    }

    @Test
    void storedFileIsKeptWhenTheTransactionCommits() {
        when(storage.store(upload)).thenReturn("new.png");

        assertEquals("new.png", files.store(upload));
        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(storage, never()).delete(any());
    }

    @Test
    void storedFileIsDeletedWhenTheTransactionRollsBack() {
        when(storage.store(upload)).thenReturn("new.png");

        files.store(upload);
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).delete("new.png");
    }

    @Test
    void unusedFileIsDeletedOnlyAfterCommit() {
        files.discardIfUnused("old.png");

        verify(storage, never()).delete("old.png");
        complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(storage).delete("old.png");
    }

    @Test
    void unusedFileSurvivesARollback() {
        files.discardIfUnused("old.png");

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage, never()).delete(any());
    }

    @Test
    void fileStillShownByAnOrderIsKept() {
        when(orderItems.existsByImageFilename("old.png")).thenReturn(true);

        files.discardIfUnused("old.png");
        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(storage, never()).delete(any());
    }

    @Test
    void fileStillUsedByAProductImageIsKept() {
        when(images.existsByFilename("old.png")).thenReturn(true);

        files.discardIfUnused("old.png");
        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(storage, never()).delete(any());
    }

    @Test
    void fileStillUsedByACategoryIsKept() {
        when(categories.existsByImageFilename("old.png")).thenReturn(true);

        files.discardIfUnused("old.png");
        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(storage, never()).delete(any());
    }

    @Test
    void nullFilenameIsIgnored() {
        files.discardIfUnused(null);
        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(storage, never()).delete(any());
    }
}
