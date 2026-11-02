package com.mayilboutique.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.images")
public class ImageStorageProperties {

    /** Directory (outside the project) that holds the uploaded image files. */
    private String dir;
    /** Largest accepted upload, in bytes. */
    private long maxSizeBytes = 5L * 1024 * 1024;

    /** Also keep every image in the database and restore missing files from it (for hosts with an ephemeral disk). */
    private boolean persistInDb = false;

    public boolean isPersistInDb() {
        return persistInDb;
    }

    public void setPersistInDb(boolean persistInDb) {
        this.persistInDb = persistInDb;
    }

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
