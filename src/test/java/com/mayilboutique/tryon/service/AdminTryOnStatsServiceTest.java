package com.mayilboutique.tryon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.JobStatus;
import com.mayilboutique.tryon.TryOnProperties;
import com.mayilboutique.tryon.dto.TryOnStatsResponse;
import com.mayilboutique.tryon.provider.TryOnCapabilities;
import com.mayilboutique.tryon.repository.VirtualTryOnJobRepository;

class AdminTryOnStatsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private VirtualTryOnJobRepository jobs;
    private TryOnProperties props;
    private AdminTryOnStatsService service;

    @BeforeEach
    void setUp() {
        jobs = mock(VirtualTryOnJobRepository.class);
        TryOnCapabilities capabilities = mock(TryOnCapabilities.class);
        when(capabilities.providerConfigured()).thenReturn(true);
        when(capabilities.supportedTypes()).thenReturn(List.of(GarmentType.UPPER_BODY, GarmentType.DRESS));
        props = new TryOnProperties();
        props.setEnabled(true);
        service = new AdminTryOnStatsService(jobs, capabilities, props, Clock.fixed(NOW, ZoneOffset.UTC));

        when(jobs.countByCreatedAtAfter(any())).thenReturn(10L);
        when(jobs.countByStatusAndCreatedAtAfter(any(), any())).thenReturn(2L);
        when(jobs.countSucceededSince(any(), any())).thenReturn(6L);
        when(jobs.countUnfinishedSince(any(), any())).thenAnswer(i -> {
            Collection<JobStatus> s = i.getArgument(1);
            return s.contains(JobStatus.PENDING) ? 1L : 1L;
        });
        when(jobs.countCustomersSince(any())).thenReturn(4L);
        when(jobs.averageSecondsSince(any())).thenReturn(12.5);
        when(jobs.countByGarmentTypeSince(any())).thenReturn(List.<Object[]>of(new Object[] {GarmentType.DRESS, 7L}));
        when(jobs.topProductsSince(any(), any(Pageable.class))).thenReturn(List.<Object[]>of(new Object[] {"Peacock Kurti", 5L}));
    }

    @Test
    void summarisesTheNumbers() {
        TryOnStatsResponse r = service.stats(30);
        assertEquals(10, r.total());
        assertEquals(6, r.succeeded());
        assertEquals(2, r.failed());
        assertEquals(0.75, r.successRate(), 1e-9);
        assertEquals(12.5, r.averageSeconds());
        assertEquals(4, r.uniqueCustomers());
        assertEquals("DRESS", r.byGarmentType().get(0).label());
        assertEquals(7, r.byGarmentType().get(0).count());
        assertEquals("Peacock Kurti", r.topProducts().get(0).label());
        assertEquals(true, r.providerConfigured());
    }

    @Test
    void costIsAnEstimateOnlyWhenConfigured() {
        assertNull(service.stats(30).estimatedCost());
        props.setCostPerSuccessfulJob(1.5);
        assertEquals(new BigDecimal("9.00"), service.stats(30).estimatedCost());
    }

    @Test
    void successRateIsNullWithNothingFinished() {
        when(jobs.countByStatusAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(jobs.countSucceededSince(any(), any())).thenReturn(0L);
        assertNull(service.stats(7).successRate());
    }

    @Test
    void rejectsSillyPeriods() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class, () -> service.stats(0)).getHttpStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class, () -> service.stats(366)).getHttpStatus());
    }
}
