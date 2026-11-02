package com.mayilboutique.tryon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.JobStatus;
import com.mayilboutique.tryon.TryOnProperties;
import com.mayilboutique.tryon.dto.TryOnJobResponse;
import com.mayilboutique.tryon.dto.TryOnProductResponse;
import com.mayilboutique.tryon.entity.VirtualTryOnJob;
import com.mayilboutique.tryon.provider.ProviderAssets;
import com.mayilboutique.tryon.repository.VirtualTryOnJobRepository;

class TryOnJobServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private static final Long ME = 1L;
    private static final Long OTHER = 2L;

    private VirtualTryOnJobRepository jobs;
    private TryOnConfigService config;
    private TryOnFileStorage storage;
    private TryOnProperties props;
    private ProviderAssets providerAssets;
    private TryOnJobService service;
    private final MockMultipartFile photo = new MockMultipartFile("photo", "p.jpg", "image/jpeg", new byte[] {1});

    @BeforeEach
    void setUp() {
        jobs = mock(VirtualTryOnJobRepository.class);
        config = mock(TryOnConfigService.class);
        storage = mock(TryOnFileStorage.class);
        props = new TryOnProperties();
        props.setEnabled(true);
        providerAssets = mock(ProviderAssets.class);
        service = new TryOnJobService(jobs, config, storage, props, Clock.fixed(NOW, ZoneOffset.UTC), providerAssets);

        ProductResponse product = mock(ProductResponse.class);
        when(product.getName()).thenReturn("Peacock Kurti");
        when(config.getEligible(7L)).thenReturn(new TryOnProductResponse(product, GarmentType.UPPER_BODY, 10L));
        when(storage.storePhoto(photo)).thenReturn("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg");
        when(jobs.save(any(VirtualTryOnJob.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static void assertStatus(HttpStatus status, Runnable action) {
        assertEquals(status, assertThrows(BusinessException.class, action::run).getHttpStatus());
    }

    private VirtualTryOnJob job(JobStatus status, Instant expiresAt) {
        VirtualTryOnJob j = new VirtualTryOnJob(ME, 7L, "Peacock Kurti", GarmentType.UPPER_BODY,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg", NOW.minusSeconds(60), expiresAt);
        j.setStatus(status);
        when(jobs.findByIdAndCustomerId(5L, ME)).thenReturn(Optional.of(j));
        return j;
    }

    @Test
    void createStoresPhotoAndStartsPendingJobWithExpiry() {
        TryOnJobResponse r = service.create(ME, 7L, photo, true);
        assertEquals(JobStatus.PENDING, r.status());
        assertEquals("Peacock Kurti", r.productName());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), r.expiresAt());
    }

    @Test
    void createIsRefusedWhileNoProviderIsConnected() {
        props.setEnabled(false);
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> service.create(ME, 7L, photo, true));
        verify(storage, never()).storePhoto(any());
    }

    @Test
    void createNeedsExplicitConsent() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.create(ME, 7L, photo, false));
        verify(storage, never()).storePhoto(any());
    }

    @Test
    void createRejectsIneligibleProduct() {
        when(config.getEligible(8L)).thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "no"));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.create(ME, 8L, photo, true));
    }

    @Test
    void secondJobWhileOneIsRunningIsAConflict() {
        when(jobs.existsByCustomerIdAndStatusInAndExpiresAtAfter(eq(ME), anyCollection(), eq(NOW))).thenReturn(true);
        assertStatus(HttpStatus.CONFLICT, () -> service.create(ME, 7L, photo, true));
        verify(storage, never()).storePhoto(any());
    }

    @Test
    void rateLimitIs429() {
        when(jobs.countByCustomerIdAndCreatedAtAfter(eq(ME), eq(NOW.minus(Duration.ofHours(1))))).thenReturn(5L);
        assertStatus(HttpStatus.TOO_MANY_REQUESTS, () -> service.create(ME, 7L, photo, true));
        verify(storage, never()).storePhoto(any());
    }

    @Test
    void storedPhotoIsRemovedIfSavingTheJobFails() {
        when(jobs.save(any(VirtualTryOnJob.class))).thenThrow(new IllegalStateException("db down"));
        assertThrows(IllegalStateException.class, () -> service.create(ME, 7L, photo, true));
        verify(storage).delete("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg");
    }

    @Test
    void anotherCustomersJobIsNotFound() {
        job(JobStatus.COMPLETED, NOW.plusSeconds(600));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.get(OTHER, 5L));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.result(OTHER, 5L));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.delete(OTHER, 5L));
    }

    @Test
    void pastExpiryReadsAsExpiredAndResultIsGone() {
        VirtualTryOnJob j = job(JobStatus.COMPLETED, NOW.minusSeconds(1));
        j.setGeneratedImagePath("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png");
        assertEquals(JobStatus.EXPIRED, service.get(ME, 5L).status());
        assertFalse(service.get(ME, 5L).resultAvailable());
        assertStatus(HttpStatus.GONE, () -> service.result(ME, 5L));
        verify(storage, never()).find(any());
    }

    @Test
    void resultBeforeCompletionIsConflict() {
        job(JobStatus.PROCESSING, NOW.plusSeconds(600));
        assertStatus(HttpStatus.CONFLICT, () -> service.result(ME, 5L));
    }

    @Test
    void completedResultIsServedWhileFresh() {
        VirtualTryOnJob j = job(JobStatus.COMPLETED, NOW.plusSeconds(600));
        j.setGeneratedImagePath("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png");
        Path p = Path.of("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png");
        when(storage.find("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png")).thenReturn(Optional.of(p));
        when(storage.mediaTypeFor(any())).thenReturn(MediaType.IMAGE_PNG);
        assertEquals(p, service.result(ME, 5L).path());
        assertTrue(service.get(ME, 5L).resultAvailable());
    }

    @Test
    void deleteRemovesBothFilesAndIsRepeatable() {
        VirtualTryOnJob j = job(JobStatus.COMPLETED, NOW.plusSeconds(600));
        j.setGeneratedImagePath("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png");
        service.delete(ME, 5L);
        verify(storage).delete("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg");
        verify(storage).delete("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png");
        assertNull(j.getOriginalPhotoPath());
        assertNull(j.getGeneratedImagePath());
        assertEquals(JobStatus.EXPIRED, j.getStatus());
        service.delete(ME, 5L); // second call must not fail
    }

    @Test
    void deletingARunningJobCancelsIt() {
        VirtualTryOnJob j = job(JobStatus.PROCESSING, NOW.plusSeconds(600));
        service.delete(ME, 5L);
        assertEquals(JobStatus.CANCELLED, j.getStatus());
    }

    @Test
    void completeStoresResultAndShortensRetention() {
        VirtualTryOnJob j = job(JobStatus.PROCESSING, NOW.plusSeconds(1700));
        when(jobs.findById(5L)).thenReturn(Optional.of(j));
        when(storage.storeResult(any())).thenReturn("cccccccccccccccccccccccccccccccc.png");
        service.complete(5L, new byte[] {1});
        assertEquals(JobStatus.COMPLETED, j.getStatus());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), j.getExpiresAt());
        assertEquals("cccccccccccccccccccccccccccccccc.png", j.getGeneratedImagePath());
    }

    @Test
    void completeIsIgnoredForACancelledJob() {
        VirtualTryOnJob j = job(JobStatus.CANCELLED, NOW);
        when(jobs.findById(5L)).thenReturn(Optional.of(j));
        service.complete(5L, new byte[] {1});
        verify(storage, never()).storeResult(any());
        assertEquals(JobStatus.CANCELLED, j.getStatus());
    }

    @Test
    void failKeepsASafeMessage() {
        VirtualTryOnJob j = job(JobStatus.PROCESSING, NOW.plusSeconds(600));
        when(jobs.findById(5L)).thenReturn(Optional.of(j));
        service.fail(5L, "We could not create your preview. Please try again.");
        assertEquals(JobStatus.FAILED, j.getStatus());
        assertEquals("We could not create your preview. Please try again.", j.getErrorMessage());
    }
}
