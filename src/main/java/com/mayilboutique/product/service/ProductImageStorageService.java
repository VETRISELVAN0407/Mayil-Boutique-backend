package com.mayilboutique.product.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mayilboutique.core.config.ImageStorageProperties;
import com.mayilboutique.core.exception.BusinessException;

/**
 * Stores product images in a directory outside the project. Callers only ever see generated filenames;
 * the original upload name and the directory location are never used in paths, responses or logs.
 */
@Service
public class ProductImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ProductImageStorageService.class);

    /** Only names this service generates are accepted for reads and deletes. */
    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f]{32}\\.(jpg|png|webp)$");

    private final Path baseDir;
    private final long maxSizeBytes;

    private final boolean persistInDb;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate newTx;

    public ProductImageStorageService(ImageStorageProperties properties) {
        this(properties, null, null);
    }

    @Autowired
    public ProductImageStorageService(ImageStorageProperties properties, JdbcTemplate jdbc,
                                      PlatformTransactionManager txManager) {
        this.persistInDb = properties.isPersistInDb() && jdbc != null;
        this.jdbc = jdbc;
        if (txManager != null) {
            this.newTx = new TransactionTemplate(txManager);
            this.newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        } else {
            this.newTx = null;
        }
        if (properties.getDir() == null || properties.getDir().isBlank()) {
            throw new IllegalStateException("app.images.dir (PRODUCT_IMAGE_DIR) must be configured");
        }
        this.baseDir = Paths.get(properties.getDir()).toAbsolutePath().normalize();
        this.maxSizeBytes = properties.getMaxSizeBytes();
    }

    /** Validates and saves the upload, returning the generated filename to persist. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Image file is empty");
        }
        if (file.getSize() > maxSizeBytes) {
            throw tooLarge();
        }
        byte[] data = readLimited(file);
        String extension = detectExtension(data);
        if (extension == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG and WebP images are allowed");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(filename).normalize();
            if (!target.startsWith(baseDir)) {
                throw new IllegalStateException("Resolved image path escaped the image directory");
            }
            Path temp = Files.createTempFile(baseDir, ".upload-", ".tmp");
            try {
                Files.write(temp, data);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temp);
            }
            if (persistInDb) {
                jdbc.update("INSERT INTO stored_images (filename, content) VALUES (?, ?)", filename, data);
            }
        } catch (IOException | RuntimeException ex) {
            log.error("Failed to store product image ({})", ex.getClass().getSimpleName());
            try {
                Files.deleteIfExists(baseDir.resolve(filename));
            } catch (IOException ignored) {
                // best effort
            }
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the image");
        }
        return filename;
    }

    /** Best-effort delete; never throws, so cleanup cannot mask the original outcome. */
    public void delete(String filename) {
        if (filename == null || !STORED_NAME.matcher(filename).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(baseDir.resolve(filename));
        } catch (IOException ex) {
            log.warn("Could not delete product image file ({})", ex.getClass().getSimpleName());
        }
        if (persistInDb) {
            try {
                // Own transaction: this runs after the caller's transaction has completed.
                newTx.executeWithoutResult(status -> jdbc.update("DELETE FROM stored_images WHERE filename = ?", filename));
            } catch (RuntimeException ex) {
                log.warn("Could not delete stored image row ({})", ex.getClass().getSimpleName());
            }
        }
    }

    /** Re-creates a missing file from the database copy (after a redeploy wiped the disk). */
    private void restoreFromDb(String filename, Path path) {
        try {
            byte[] data = jdbc.query("SELECT content FROM stored_images WHERE filename = ?",
                    rs -> rs.next() ? rs.getBytes(1) : null, filename);
            if (data == null) {
                return;
            }
            Files.createDirectories(baseDir);
            Path temp = Files.createTempFile(baseDir, ".restore-", ".tmp");
            try {
                Files.write(temp, data);
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException | RuntimeException ex) {
            log.warn("Could not restore image from database ({})", ex.getClass().getSimpleName());
        }
    }

    /** Returns the file for a stored image, or throws 404. Rejects anything not generated by {@link #store}. */
    public Path resolve(String filename) {
        if (filename == null || !STORED_NAME.matcher(filename).matches()) {
            throw notFound();
        }
        Path path = baseDir.resolve(filename).normalize();
        if (!path.startsWith(baseDir)) {
            throw notFound();
        }
        if (!Files.isRegularFile(path) && persistInDb) {
            restoreFromDb(filename, path);
        }
        if (!Files.isRegularFile(path)) {
            throw notFound();
        }
        try {
            // Guards against a symlink inside the folder pointing elsewhere.
            if (!path.toRealPath().startsWith(baseDir.toRealPath())) {
                throw notFound();
            }
        } catch (IOException ex) {
            throw notFound();
        }
        return path;
    }

    public MediaType mediaTypeFor(String filename) {
        if (filename.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (filename.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    private byte[] readLimited(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] data = in.readNBytes((int) maxSizeBytes + 1);
            if (data.length > maxSizeBytes) {
                throw tooLarge();
            }
            if (data.length == 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Image file is empty");
            }
            return data;
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Could not read the uploaded image");
        }
    }

    /** Identifies the format from the file's own bytes; the client-supplied type and name are not trusted. */
    private static String detectExtension(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        if (d.length >= 8 && Arrays.equals(Arrays.copyOfRange(d, 0, 8), png)) {
            return "png";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private BusinessException tooLarge() {
        return new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE,
                "Image must not exceed " + (maxSizeBytes / (1024 * 1024)) + " MB");
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "Image not found");
    }
}
