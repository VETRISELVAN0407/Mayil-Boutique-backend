package com.mayilboutique.tryon.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.tryon.JobStatus;
import com.mayilboutique.tryon.TryOnProperties;
import com.mayilboutique.tryon.dto.TryOnStatsResponse;
import com.mayilboutique.tryon.dto.TryOnStatsResponse.Count;
import com.mayilboutique.tryon.provider.TryOnCapabilities;
import com.mayilboutique.tryon.repository.VirtualTryOnJobRepository;

@Service
public class AdminTryOnStatsService {

    static final int MAX_DAYS = 365;
    private static final int TOP_PRODUCTS = 5;

    private final VirtualTryOnJobRepository jobs;
    private final TryOnCapabilities capabilities;
    private final TryOnProperties props;
    private final Clock clock;

    public AdminTryOnStatsService(VirtualTryOnJobRepository jobs, TryOnCapabilities capabilities,
                                  TryOnProperties props, Clock clock) {
        this.jobs = jobs;
        this.capabilities = capabilities;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TryOnStatsResponse stats(int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "days must be between 1 and " + MAX_DAYS);
        }
        Instant since = clock.instant().minus(Duration.ofDays(days));

        long total = jobs.countByCreatedAtAfter(since);
        long failed = jobs.countByStatusAndCreatedAtAfter(JobStatus.FAILED, since);
        long succeeded = jobs.countSucceededSince(since, JobStatus.FAILED);
        long abandoned = jobs.countUnfinishedSince(since, EnumSet.of(JobStatus.CANCELLED, JobStatus.EXPIRED));
        long inProgress = jobs.countUnfinishedSince(since, EnumSet.of(JobStatus.PENDING, JobStatus.PROCESSING));
        Double avg = jobs.averageSecondsSince(since);
        Double rate = succeeded + failed == 0 ? null : (double) succeeded / (succeeded + failed);

        BigDecimal cost = props.getCostPerSuccessfulJob() > 0
                ? BigDecimal.valueOf(props.getCostPerSuccessfulJob()).multiply(BigDecimal.valueOf(succeeded))
                        .setScale(2, RoundingMode.HALF_UP)
                : null;

        return new TryOnStatsResponse(days, props.isEnabled(), capabilities.providerConfigured(),
                capabilities.supportedTypes(), total, succeeded, failed, abandoned, inProgress,
                jobs.countCustomersSince(since), avg, rate, cost,
                counts(jobs.countByGarmentTypeSince(since)),
                counts(jobs.topProductsSince(since, PageRequest.of(0, TOP_PRODUCTS))));
    }

    private static List<Count> counts(List<Object[]> rows) {
        return rows.stream().map(r -> new Count(String.valueOf(r[0]), ((Number) r[1]).longValue())).toList();
    }
}
