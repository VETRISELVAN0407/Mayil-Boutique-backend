package com.mayilboutique.tryon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.Pageable;

import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.JobStatus;
import com.mayilboutique.tryon.TryOnProperties;
import com.mayilboutique.tryon.entity.VirtualTryOnJob;
import com.mayilboutique.tryon.provider.ProviderAssets;
import com.mayilboutique.tryon.repository.VirtualTryOnJobRepository;

/** Uses a real temp folder so deletion (and deleting twice) is actually exercised. */
class TryOnCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private static final String PHOTO = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg";
    private static final String RESULT = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png";

    @TempDir
    Path dir;

    private VirtualTryOnJobRepository jobs;
    private ProviderAssets providerAssets;
    private TryOnCleanupService cleanup;
    private final List<VirtualTryOnJob> due = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TryOnProperties props = new TryOnProperties();
        props.setDir(dir.toString());
        jobs = mock(VirtualTryOnJobRepository.class);
        when(jobs.findDueForCleanup(any(), any(), any(Pageable.class))).thenAnswer(i -> List.copyOf(due));
        when(jobs.existsByOriginalPhotoPathOrGeneratedImagePath(anyString(), anyString())).thenReturn(false);
        providerAssets = mock(ProviderAssets.class);
        cleanup = new TryOnCleanupService(jobs, new TryOnFileStorage(props), props, Clock.fixed(NOW, ZoneOffset.UTC), providerAssets);
    }

    private VirtualTryOnJob job(JobStatus status, String photo, String result) throws Exception {
        VirtualTryOnJob j = new VirtualTryOnJob(1L, 7L, "Kurti", GarmentType.DRESS, photo, NOW.minusSeconds(3600),
                NOW.minusSeconds(1));
        j.setStatus(status);
        j.setGeneratedImagePath(result);
        if (photo != null) Files.write(dir.resolve(photo), new byte[] {1});
        if (result != null) Files.write(dir.resolve(result), new byte[] {1});
        due.add(j);
        return j;
    }

    @Test
    void expiredCompletedJobLosesBothFilesAndBecomesExpired() throws Exception {
        VirtualTryOnJob j = job(JobStatus.COMPLETED, PHOTO, RESULT);
        cleanup.cleanup();
        assertFalse(Files.exists(dir.resolve(PHOTO)));
        assertFalse(Files.exists(dir.resolve(RESULT)));
        assertNull(j.getOriginalPhotoPath());
        assertEquals(JobStatus.EXPIRED, j.getStatus());
    }

    @Test
    void abandonedRunningJobBecomesExpired() throws Exception {
        VirtualTryOnJob j = job(JobStatus.PENDING, PHOTO, null);
        cleanup.cleanup();
        assertEquals(JobStatus.EXPIRED, j.getStatus());
        assertFalse(Files.exists(dir.resolve(PHOTO)));
    }

    @Test
    void failedJobKeepsItsFailedStatusButLosesItsPhoto() throws Exception {
        VirtualTryOnJob j = job(JobStatus.FAILED, PHOTO, null);
        cleanup.cleanup();
        assertEquals(JobStatus.FAILED, j.getStatus());
        assertFalse(Files.exists(dir.resolve(PHOTO)));
    }

    @Test
    void missingFilesAndRepeatedRunsAreHarmless() throws Exception {
        VirtualTryOnJob j = job(JobStatus.COMPLETED, PHOTO, RESULT);
        Files.delete(dir.resolve(PHOTO)); // already gone
        cleanup.cleanup();
        cleanup.cleanup(); // the same job is offered again; nothing left to delete
        assertEquals(JobStatus.EXPIRED, j.getStatus());
    }

    @Test
    void oldUnreferencedFilesAreSweptButReferencedOnesStay() throws Exception {
        Path stray = Files.write(dir.resolve("cccccccccccccccccccccccccccccccc.jpg"), new byte[] {1});
        Path kept = Files.write(dir.resolve("dddddddddddddddddddddddddddddddd.jpg"), new byte[] {1});
        Files.setLastModifiedTime(stray, java.nio.file.attribute.FileTime.from(NOW.minusSeconds(86400)));
        Files.setLastModifiedTime(kept, java.nio.file.attribute.FileTime.from(NOW.minusSeconds(86400)));
        when(jobs.existsByOriginalPhotoPathOrGeneratedImagePath("dddddddddddddddddddddddddddddddd.jpg",
                "dddddddddddddddddddddddddddddddd.jpg")).thenReturn(true);

        assertEquals(1, cleanup.cleanup());

        assertFalse(Files.exists(stray));
        assertTrue(Files.exists(kept));
    }

    @Test
    void freshUnreferencedFilesAreLeftAloneBecauseAnUploadMayBeInFlight() throws Exception {
        Path fresh = Files.write(dir.resolve("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee.jpg"), new byte[] {1});
        Files.setLastModifiedTime(fresh, java.nio.file.attribute.FileTime.from(NOW.minusSeconds(60)));
        assertEquals(0, cleanup.cleanup());
        assertTrue(Files.exists(fresh));
    }
}
