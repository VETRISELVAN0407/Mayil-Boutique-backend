package com.mayilboutique.tryon;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tryon")
public class TryOnProperties {

    /** Master switch. While false, creating jobs answers 503 (no AI provider is connected yet). */
    private boolean enabled = false;
    /** Private folder for customer photos and generated results. Must NOT be a served or backed-up-for-keeps folder. */
    private String dir;
    /** Largest accepted customer photo, in bytes (the multipart transport cap also applies). */
    private long maxPhotoBytes = 5L * 1024 * 1024;
    private int minDimension = 256;
    private int maxDimension = 6000;
    /** Minutes a finished (completed or failed) job's files are kept. */
    private long retentionMinutes = 30;
    /** Minutes an unfinished job may run before it is treated as abandoned and cleaned up. */
    private long activeJobMinutes = 30;
    private int maxJobsPerHour = 5;
    private int maxJobsPerDay = 20;
    /** Engine used to generate previews: {@code none} (default) or {@code selfhosted} (our tryon-service). */
    private String provider = "none";
    /** Base URL of the self-hosted try-on service, e.g. http://127.0.0.1:8090. */
    private String serviceUrl;
    private String serviceApiKey;
    private int serviceTimeoutMs = 30_000;
    /** Starting a job at the provider is attempted this many times before the job fails. */
    private int maxStartAttempts = 3;
    private long workerIntervalMs = 2_000;
    /** Optional: what one successful preview costs you (e.g. GPU cost per image, in INR). Only used for the admin estimate. */
    private double costPerSuccessfulJob = 0;
    /** Pause between cleanup runs. */
    private long cleanupIntervalMs = 5 * 60 * 1000L;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }
    public long getMaxPhotoBytes() { return maxPhotoBytes; }
    public void setMaxPhotoBytes(long maxPhotoBytes) { this.maxPhotoBytes = maxPhotoBytes; }
    public int getMinDimension() { return minDimension; }
    public void setMinDimension(int minDimension) { this.minDimension = minDimension; }
    public int getMaxDimension() { return maxDimension; }
    public void setMaxDimension(int maxDimension) { this.maxDimension = maxDimension; }
    public long getRetentionMinutes() { return retentionMinutes; }
    public void setRetentionMinutes(long retentionMinutes) { this.retentionMinutes = retentionMinutes; }
    public long getActiveJobMinutes() { return activeJobMinutes; }
    public void setActiveJobMinutes(long activeJobMinutes) { this.activeJobMinutes = activeJobMinutes; }
    public int getMaxJobsPerHour() { return maxJobsPerHour; }
    public void setMaxJobsPerHour(int maxJobsPerHour) { this.maxJobsPerHour = maxJobsPerHour; }
    public int getMaxJobsPerDay() { return maxJobsPerDay; }
    public void setMaxJobsPerDay(int maxJobsPerDay) { this.maxJobsPerDay = maxJobsPerDay; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }
    public String getServiceApiKey() { return serviceApiKey; }
    public void setServiceApiKey(String serviceApiKey) { this.serviceApiKey = serviceApiKey; }
    public int getServiceTimeoutMs() { return serviceTimeoutMs; }
    public void setServiceTimeoutMs(int serviceTimeoutMs) { this.serviceTimeoutMs = serviceTimeoutMs; }
    public int getMaxStartAttempts() { return maxStartAttempts; }
    public void setMaxStartAttempts(int maxStartAttempts) { this.maxStartAttempts = maxStartAttempts; }
    public long getWorkerIntervalMs() { return workerIntervalMs; }
    public void setWorkerIntervalMs(long workerIntervalMs) { this.workerIntervalMs = workerIntervalMs; }
    public double getCostPerSuccessfulJob() { return costPerSuccessfulJob; }
    public void setCostPerSuccessfulJob(double costPerSuccessfulJob) { this.costPerSuccessfulJob = costPerSuccessfulJob; }
    public long getCleanupIntervalMs() { return cleanupIntervalMs; }
    public void setCleanupIntervalMs(long cleanupIntervalMs) { this.cleanupIntervalMs = cleanupIntervalMs; }
}
