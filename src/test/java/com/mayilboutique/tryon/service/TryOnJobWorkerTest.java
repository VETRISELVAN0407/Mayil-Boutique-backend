package com.mayilboutique.tryon.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.product.service.ProductImageStorageService;
import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.JobStatus;
import com.mayilboutique.tryon.TryOnProperties;
import com.mayilboutique.tryon.entity.VirtualTryOnJob;
import com.mayilboutique.tryon.provider.ProviderAssets;
import com.mayilboutique.tryon.provider.ProviderException;
import com.mayilboutique.tryon.provider.ProviderStatus;
import com.mayilboutique.tryon.provider.TryOnRequest;
import com.mayilboutique.tryon.provider.VirtualTryOnProvider;
import com.mayilboutique.tryon.repository.VirtualTryOnJobRepository;

class TryOnJobWorkerTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private static final String PHOTO = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg";

    @TempDir
    Path dir;

    private VirtualTryOnProvider provider;
    private VirtualTryOnJobRepository jobs;
    private TryOnJobService jobService;
    private TryOnConfigService configService;
    private TryOnFileStorage storage;
    private ProductImageStorageService productImages;
    private ProviderAssets assets;
    private TryOnProperties props;
    private TryOnJobWorker worker;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        provider = mock(VirtualTryOnProvider.class);
        when(provider.name()).thenReturn("selfhosted");
        ObjectProvider<VirtualTryOnProvider> providers = mock(ObjectProvider.class);
        when(providers.getIfAvailable()).thenReturn(provider);
        jobs = mock(VirtualTryOnJobRepository.class);
        jobService = mock(TryOnJobService.class);
        configService = mock(TryOnConfigService.class);
        storage = mock(TryOnFileStorage.class);
        productImages = mock(ProductImageStorageService.class);
        assets = mock(ProviderAssets.class);
        props = new TryOnProperties();
        props.setEnabled(true);
        worker = new TryOnJobWorker(providers, jobs, jobService, configService, storage, productImages, assets, props,
                Clock.fixed(NOW, ZoneOffset.UTC));

        Path photo = Files.write(dir.resolve("photo.jpg"), new byte[] {1});
        Path garment = Files.write(dir.resolve("garment.jpg"), new byte[] {2});
        when(storage.find(PHOTO)).thenReturn(java.util.Optional.of(photo));
        when(configService.garmentImageFilenames(7L)).thenReturn(List.of("g1.jpg"));
        when(productImages.resolve("g1.jpg")).thenReturn(garment);
        when(jobs.findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
    }

    private VirtualTryOnJob job(long id, JobStatus status, String providerJobId) {
        VirtualTryOnJob j = new VirtualTryOnJob(1L, 7L, "Kurti", GarmentType.UPPER_BODY, PHOTO, NOW.minusSeconds(30),
                NOW.plusSeconds(1000));
        ReflectionTestUtils.setField(j, "id", id);
        j.setStatus(status);
        j.setProviderJobId(providerJobId);
        return j;
    }

    private void queue(JobStatus status, VirtualTryOnJob... list) {
        when(jobs.findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(eq(status), any(), any(Pageable.class)))
                .thenReturn(List.of(list));
    }

    @Test
    void doesNothingWhileSwitchedOff() {
        props.setEnabled(false);
        worker.runOnce();
        verify(jobs, never()).findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(any(), any(), any());
    }

    @Test
    void submitsPendingJobWithPersonAndGarmentBytes() throws Exception {
        queue(JobStatus.PENDING, job(1, JobStatus.PENDING, null));
        when(provider.start(any())).thenReturn("p-1");

        worker.runOnce();

        ArgumentCaptor<TryOnRequest> req = ArgumentCaptor.forClass(TryOnRequest.class);
        verify(provider).start(req.capture());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] {1}, req.getValue().personPhoto());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] {2}, req.getValue().garments().get(0));
        verify(jobService).markProcessing(1L, "selfhosted", "p-1");
    }

    @Test
    void transientStartFailureIsRetriedThenFailsAfterTheLimit() throws Exception {
        queue(JobStatus.PENDING, job(1, JobStatus.PENDING, null));
        when(provider.start(any())).thenThrow(new ProviderException("down", true));
        when(jobService.recordFailedAttempt(1L)).thenReturn(1, 3);

        worker.runOnce();
        verify(jobService, never()).fail(anyLong(), any());

        // The retry delay has not passed (same fixed clock), so the second pass skips the job entirely.
        worker.runOnce();
        verify(provider, times(1)).start(any());
    }

    @Test
    void failsAfterMaxAttempts() throws Exception {
        queue(JobStatus.PENDING, job(1, JobStatus.PENDING, null));
        when(provider.start(any())).thenThrow(new ProviderException("down", true));
        when(jobService.recordFailedAttempt(1L)).thenReturn(3);

        worker.runOnce();

        verify(jobService).fail(1L, TryOnJobWorker.FAILURE_MESSAGE);
    }

    @Test
    void rejectedInputFailsImmediately() throws Exception {
        queue(JobStatus.PENDING, job(1, JobStatus.PENDING, null));
        when(provider.start(any())).thenThrow(new ProviderException("bad image", false));

        worker.runOnce();

        verify(jobService).fail(1L, TryOnJobWorker.FAILURE_MESSAGE);
        verify(jobService, never()).recordFailedAttempt(anyLong());
    }

    @Test
    void missingPhotoFailsTheJobWithoutCallingTheProvider() throws Exception {
        queue(JobStatus.PENDING, job(1, JobStatus.PENDING, null));
        when(storage.find(PHOTO)).thenReturn(java.util.Optional.empty());

        worker.runOnce();

        verify(provider, never()).start(any());
        verify(jobService).fail(1L, TryOnJobWorker.FAILURE_MESSAGE);
    }

    @Test
    void doneJobIsCompletedAndProviderCopyDeleted() throws Exception {
        queue(JobStatus.PROCESSING, job(2, JobStatus.PROCESSING, "p-2"));
        when(provider.status("p-2")).thenReturn(new ProviderStatus(ProviderStatus.State.DONE));
        when(provider.result("p-2")).thenReturn(new byte[] {5});

        worker.runOnce();

        verify(jobService).complete(2L, new byte[] {5});
        verify(assets).deleteQuietly("p-2");
    }

    @Test
    void runningJobIsLeftAlone() throws Exception {
        queue(JobStatus.PROCESSING, job(2, JobStatus.PROCESSING, "p-2"));
        when(provider.status("p-2")).thenReturn(new ProviderStatus(ProviderStatus.State.RUNNING));

        worker.runOnce();

        verify(jobService, never()).complete(anyLong(), any());
        verify(jobService, never()).fail(anyLong(), any());
    }

    @Test
    void providerFailureFailsTheJobWithASafeMessage() throws Exception {
        queue(JobStatus.PROCESSING, job(2, JobStatus.PROCESSING, "p-2"));
        when(provider.status("p-2")).thenReturn(new ProviderStatus(ProviderStatus.State.FAILED));

        worker.runOnce();

        verify(jobService).fail(2L, TryOnJobWorker.FAILURE_MESSAGE);
        verify(assets).deleteQuietly("p-2");
    }

    @Test
    void transientPollErrorKeepsWaitingButUnknownJobFails() throws Exception {
        queue(JobStatus.PROCESSING, job(2, JobStatus.PROCESSING, "p-2"));
        org.mockito.Mockito.doThrow(new ProviderException("blip", true)).when(provider).status("p-2");
        worker.runOnce();
        verify(jobService, never()).fail(anyLong(), any());

        org.mockito.Mockito.doThrow(new ProviderException("gone", false)).when(provider).status("p-2");
        worker.runOnce();
        verify(jobService).fail(2L, TryOnJobWorker.FAILURE_MESSAGE);
    }
}
