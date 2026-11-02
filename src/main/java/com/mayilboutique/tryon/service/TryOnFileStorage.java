package com.mayilboutique.tryon.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.tryon.TryOnProperties;

/**
 * Private, temporary storage for customer photos and generated results. Unlike product images this folder is never
 * served by path: files are only ever read through an authenticated, ownership-checked endpoint. Callers only see
 * generated filenames; client-supplied names are never used.
 */
@Service
public class TryOnFileStorage {

    private static final Logger log = LoggerFactory.getLogger(TryOnFileStorage.class);
    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f]{32}\\.(jpg|png|webp)$");

    private final Path baseDir;
    private final TryOnProperties props;

    public TryOnFileStorage(TryOnProperties props) {
        this.props = props;
        String dir = props.getDir();
        if (dir == null || dir.isBlank()) {
            dir = System.getProperty("user.home") + "/mayil-boutique/tryon-tmp";
        }
        this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
    }

    /** Validates a customer photo (format by bytes, size, dimensions, decodable) and stores it. */
    public String storePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw bad("Please choose a photo");
        }
        if (file.getSize() > props.getMaxPhotoBytes()) {
            throw tooLarge();
        }
        byte[] data = readLimited(file);
        String ext = detectExtension(data);
        if (ext == null) {
            throw bad("Only JPG, PNG and WebP photos are allowed");
        }
        int[] size = dimensions(data, ext);
        if (size == null) {
            throw bad("This photo could not be read. Please try another one");
        }
        if (Math.min(size[0], size[1]) < props.getMinDimension()) {
            throw bad("Photo is too small. It must be at least " + props.getMinDimension() + " pixels on each side");
        }
        if (Math.max(size[0], size[1]) > props.getMaxDimension()) {
            throw bad("Photo is too large. It must be at most " + props.getMaxDimension() + " pixels on each side");
        }
        if (!ext.equals("webp") && !decodes(data, size)) {
            throw bad("This photo appears to be corrupted. Please try another one");
        }
        return write(data, ext);
    }

    /** Stores a generated result (bytes come from the AI provider, so only the format is checked). */
    public String storeResult(byte[] data) {
        String ext = data == null ? null : detectExtension(data);
        if (ext == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "The generated image could not be used");
        }
        return write(data, ext);
    }

    /** The file for a stored name, or empty if the name is not one we generated or the file is gone. */
    public Optional<Path> find(String filename) {
        if (filename == null || !STORED_NAME.matcher(filename).matches()) {
            return Optional.empty();
        }
        Path path = baseDir.resolve(filename).normalize();
        if (!path.startsWith(baseDir) || !Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(path);
    }

    /** Idempotent and best effort: a missing file or an unknown name is fine. */
    public void delete(String filename) {
        if (filename == null || !STORED_NAME.matcher(filename).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(baseDir.resolve(filename).normalize());
        } catch (IOException ex) {
            log.warn("Could not delete try-on file ({})", ex.getClass().getSimpleName());
        }
    }

    /** Names of stored files last modified before {@code cutoff}; used to sweep files no job refers to. */
    public List<String> listOlderThan(Instant cutoff) {
        if (!Files.isDirectory(baseDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(baseDir)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> STORED_NAME.matcher(p.getFileName().toString()).matches())
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant().isBefore(cutoff);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .map(p -> p.getFileName().toString())
                    .toList();
        } catch (IOException ex) {
            log.warn("Could not list try-on folder ({})", ex.getClass().getSimpleName());
            return List.of();
        }
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

    private String write(byte[] data, String ext) {
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(filename).normalize();
            if (!target.startsWith(baseDir)) {
                throw new IllegalStateException("Resolved path escaped the try-on folder");
            }
            Path temp = Files.createTempFile(baseDir, ".upload-", ".tmp");
            try {
                Files.write(temp, data);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException | RuntimeException ex) {
            log.error("Failed to store try-on file ({})", ex.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the photo");
        }
        return filename;
    }

    private byte[] readLimited(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] data = in.readNBytes((int) props.getMaxPhotoBytes() + 1);
            if (data.length > props.getMaxPhotoBytes()) {
                throw tooLarge();
            }
            if (data.length == 0) {
                throw bad("Please choose a photo");
            }
            return data;
        } catch (IOException ex) {
            throw bad("Could not read the uploaded photo");
        }
    }

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

    /** {width, height} read from the header without decoding the pixels, or null if unreadable. */
    private static int[] dimensions(byte[] d, String ext) {
        if (ext.equals("webp")) {
            return webpDimensions(d);
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(d))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                return new int[] {reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    /** Decodes a heavily sub-sampled copy: catches corrupt data without holding a full-size bitmap in memory. */
    private static boolean decodes(byte[] d, int[] size) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(d))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                ImageReadParam param = reader.getDefaultReadParam();
                int step = Math.max(1, Math.max(size[0], size[1]) / 800);
                param.setSourceSubsampling(step, step, 0, 0);
                return reader.read(0, param) != null;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }

    private static int[] webpDimensions(byte[] d) {
        if (d.length < 30) {
            return null;
        }
        String chunk = new String(d, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        switch (chunk) {
            case "VP8 ":
                return new int[] {u16(d, 26) & 0x3FFF, u16(d, 28) & 0x3FFF};
            case "VP8L": {
                if ((d[20] & 0xFF) != 0x2F) {
                    return null;
                }
                long bits = (d[21] & 0xFFL) | (d[22] & 0xFFL) << 8 | (d[23] & 0xFFL) << 16 | (d[24] & 0xFFL) << 24;
                return new int[] {(int) (bits & 0x3FFF) + 1, (int) ((bits >> 14) & 0x3FFF) + 1};
            }
            case "VP8X":
                return new int[] {u24(d, 24) + 1, u24(d, 27) + 1};
            default:
                return null;
        }
    }

    private static int u16(byte[] d, int i) {
        return (d[i] & 0xFF) | (d[i + 1] & 0xFF) << 8;
    }

    private static int u24(byte[] d, int i) {
        return u16(d, i) | (d[i + 2] & 0xFF) << 16;
    }

    private static BusinessException bad(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    private BusinessException tooLarge() {
        return new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE,
                "Photo must not exceed " + (props.getMaxPhotoBytes() / (1024 * 1024)) + " MB");
    }
}
