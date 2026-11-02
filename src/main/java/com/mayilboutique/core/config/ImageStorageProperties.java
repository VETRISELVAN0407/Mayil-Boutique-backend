package com.mayilboutique.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.images")
public class ImageStorageProperties {

    /** Directory (outside the project) that holds the uploaded image files. */
    private String dir;
    /** Largest accepted upload, in bytes. */
    private long maxSizeBytes = 5L * 1024 * 1024;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }
}
