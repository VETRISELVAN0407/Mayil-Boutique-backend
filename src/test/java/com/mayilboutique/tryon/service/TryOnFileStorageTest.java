package com.mayilboutique.tryon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.tryon.TryOnProperties;

class TryOnFileStorageTest {

    @TempDir
    Path dir;

    private TryOnProperties props;
    private TryOnFileStorage storage;

    @BeforeEach
    void setUp() {
        props = new TryOnProperties();
        props.setDir(dir.toString());
        storage = new TryOnFileStorage(props);
    }

    private static byte[] image(String format, int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    private static MockMultipartFile upload(byte[] data) {
        return new MockMultipartFile("photo", "whatever.exe", "application/octet-stream", data);
    }

    private static void assertRejected(HttpStatus status, Runnable action) {
        assertEquals(status, assertThrows(BusinessException.class, action::run).getHttpStatus());
    }

    @Test
    void storesValidJpegAndPngUnderGeneratedNames() throws Exception {
        String jpg = storage.storePhoto(upload(image("jpg", 400, 600)));
        String png = storage.storePhoto(upload(image("png", 400, 600)));
        assertTrue(jpg.matches("[0-9a-f]{32}\\.jpg"));
        assertTrue(png.matches("[0-9a-f]{32}\\.png"));
        assertTrue(storage.find(jpg).isPresent());
    }

    @Test
    void formatIsDetectedFromBytesNotFromTheClientName() {
        assertRejected(HttpStatus.BAD_REQUEST, () -> storage.storePhoto(upload("<?php echo 1; ?>".getBytes())));
    }

    @Test
    void emptyOrMissingFileIsRejected() {
        assertRejected(HttpStatus.BAD_REQUEST, () -> storage.storePhoto(upload(new byte[0])));
        assertRejected(HttpStatus.BAD_REQUEST, () -> storage.storePhoto(null));
    }

    @Test
    void tooSmallAndTooLargeDimensionsAreRejected() throws Exception {
        byte[] small = image("png", 100, 100);
        assertRejected(HttpStatus.BAD_REQUEST, () -> storage.storePhoto(upload(small)));
        props.setMaxDimension(500);
        byte[] big = image("png", 600, 400);
        assertRejected(HttpStatus.BAD_REQUEST, () -> storage.storePhoto(upload(big)));
    }

    @Test
    void oversizedFileIs413() throws Exception {
        props.setMaxPhotoBytes(50);
        byte[] data = image("png", 400, 600);
        assertRejected(HttpStatus.PAYLOAD_TOO_LARGE, () -> storage.storePhoto(upload(data)));
    }

    @Test
    void corruptImageBodyIsRejected() throws Exception {
        byte[] data = image("png", 400, 600);
        byte[] corrupt = java.util.Arrays.copyOf(data, data.length);
        for (int i = 80; i < corrupt.length - 20; i++) {
            corrupt[i] = (byte) 0x5A; // keep the header (so format and size read fine), ruin the pixel data
        }
        assertRejected(HttpStatus.BAD_REQUEST, () -> storage.storePhoto(upload(corrupt)));
    }

    @Test
    void webpHeaderDimensionsAreChecked() {
        // Minimal RIFF/WEBP "VP8X" header declaring 100x100 -> below the minimum.
        byte[] webp = new byte[40];
        System.arraycopy("RIFF".getBytes(), 0, webp, 0, 4);
        System.arraycopy("WEBP".getBytes(), 0, webp, 8, 4);
        System.arraycopy("VP8X".getBytes(), 0, webp, 12, 4);
        webp[24] = 99;
        webp[27] = 99;
        assertRejected(HttpStatus.BAD_REQUEST, () -> storage.storePhoto(upload(webp)));
    }

    @Test
    void findAndDeleteRejectUnknownNamesAndTraversal() throws Exception {
        assertTrue(storage.find("../secret.jpg").isEmpty());
        assertTrue(storage.find("abc.jpg").isEmpty());
        assertTrue(storage.find(null).isEmpty());
        Path outside = Files.createFile(dir.resolveSibling("outside-" + System.nanoTime() + ".jpg"));
        storage.delete("../" + outside.getFileName());
        assertTrue(Files.exists(outside));
        Files.delete(outside);
    }

    @Test
    void deleteIsIdempotent() throws Exception {
        String name = storage.storePhoto(upload(image("png", 400, 600)));
        storage.delete(name);
        storage.delete(name);
        assertFalse(storage.find(name).isPresent());
    }

    @Test
    void listsOnlyOldStoredFiles() throws Exception {
        String name = storage.storePhoto(upload(image("png", 400, 600)));
        assertTrue(storage.listOlderThan(Instant.now().minusSeconds(3600)).isEmpty());
        assertEquals(java.util.List.of(name), storage.listOlderThan(Instant.now().plusSeconds(60)));
    }
}
