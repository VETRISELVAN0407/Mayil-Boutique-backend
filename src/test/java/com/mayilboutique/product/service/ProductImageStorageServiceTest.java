package com.mayilboutique.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.mayilboutique.core.config.ImageStorageProperties;
import com.mayilboutique.core.exception.BusinessException;

class ProductImageStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @TempDir
    Path dir;
    private ProductImageStorageService service;

    @BeforeEach
    void setUp() {
        ImageStorageProperties props = new ImageStorageProperties();
        props.setDir(dir.resolve("images").toString());
        props.setMaxSizeBytes(1024);
        service = new ProductImageStorageService(props);
    }

    private static MockMultipartFile file(String name, String type, byte[] data) {
        return new MockMultipartFile("image", name, type, data);
    }

    private BusinessException storeFails(MockMultipartFile f) {
        return assertThrows(BusinessException.class, () -> service.store(f));
    }

    @Test
    void storesValidFormatsUnderGeneratedNamesIgnoringOriginalName() {
        String png = service.store(file("../../evil.php", "image/png", PNG));
        String jpg = service.store(file("a.png", "image/jpeg", JPEG));
        String webp = service.store(file("b.gif", "image/webp", WEBP));
        assertTrue(png.matches("[0-9a-f]{32}\\.png"));
        assertTrue(jpg.endsWith(".jpg"));
        assertTrue(webp.endsWith(".webp"));
        assertTrue(Files.isRegularFile(service.resolve(png)));
        assertEquals("image/webp", service.mediaTypeFor(webp).toString());
    }

    @Test
    void rejectsEmptyFile() {
        assertEquals(HttpStatus.BAD_REQUEST, storeFails(file("a.png", "image/png", new byte[0])).getHttpStatus());
    }

    @Test
    void rejectsNonImageEvenWithImageContentType() {
        assertEquals(HttpStatus.BAD_REQUEST,
                storeFails(file("a.png", "image/png", "<?php echo 1;".getBytes())).getHttpStatus());
    }

    @Test
    void rejectsOversizedFile() {
        byte[] big = new byte[2000];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, storeFails(file("a.png", "image/png", big)).getHttpStatus());
    }

    @Test
    void resolveRejectsTraversalAndUnknownNames() {
        for (String name : new String[] {"../secret.png", "..%2Fsecret.png", "a/b.png", "x.png", "..", ""}) {
            BusinessException ex = assertThrows(BusinessException.class, () -> service.resolve(name));
            assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        }
        BusinessException missing = assertThrows(BusinessException.class,
                () -> service.resolve("0123456789abcdef0123456789abcdef.png"));
        assertEquals(HttpStatus.NOT_FOUND, missing.getHttpStatus());
    }

    @Test
    void deleteRemovesFileAndIgnoresInvalidNames() {
        String name = service.store(file("a.png", "image/png", PNG));
        Path path = service.resolve(name);
        service.delete("../whatever");
        service.delete(null);
        service.delete(name);
        assertFalse(Files.exists(path));
    }
}
