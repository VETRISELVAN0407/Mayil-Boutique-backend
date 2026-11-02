package com.mayilboutique.tryon.provider;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.TryOnProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** Exercises the real HTTP client against a local stand-in that follows FASHN's documented API. Not the live API. */
class FashnTryOnProviderTest {

    private HttpServer server;
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> postedBody = new AtomicReference<>();
    private volatile int statusCode = 200;
    private volatile String json = "{\"id\":\"pred-1\",\"error\":null}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1", this::handle);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange ex) throws IOException {
        auth.set(ex.getRequestHeaders().getFirst("Authorization"));
        if ("POST".equals(ex.getRequestMethod())) {
            postedBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        }
        byte[] out = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(statusCode, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }

    private FashnTryOnProvider provider() {
        TryOnProperties p = new TryOnProperties();
        p.setServiceUrl("http://127.0.0.1:" + server.getAddress().getPort());
        p.setServiceApiKey("fashn-key");
        return new FashnTryOnProvider(p);
    }

    @Test
    void startSendsBearerKeyAndBase64Inputs() throws Exception {
        String id = provider().start(new TryOnRequest(GarmentType.UPPER_BODY, new byte[]{(byte) 0xFF, (byte) 0xD8, 1, 2},
                List.of(new byte[]{(byte) 0x89, 'P', 'N', 'G'})));
        assertEquals("pred-1", id);
        assertEquals("Bearer fashn-key", auth.get());
        String body = postedBody.get();
        assertTrue(body.contains("\"tryon-max\""));
        assertTrue(body.contains("\"return_base64\":true"));
        assertTrue(body.contains("data:image/jpeg;base64,"));
        assertTrue(body.contains("data:image/png;base64,"));
    }

    @Test
    void mapsStatuses() throws Exception {
        FashnTryOnProvider p = provider();
        json = "{\"id\":\"pred-1\",\"status\":\"in_queue\"}";
        assertEquals(ProviderStatus.State.QUEUED, p.status("pred-1").state());
        json = "{\"id\":\"pred-1\",\"status\":\"processing\"}";
        assertEquals(ProviderStatus.State.RUNNING, p.status("pred-1").state());
        json = "{\"id\":\"pred-1\",\"status\":\"completed\"}";
        assertEquals(ProviderStatus.State.DONE, p.status("pred-1").state());
        json = "{\"id\":\"pred-1\",\"status\":\"failed\",\"error\":{\"name\":\"X\",\"message\":\"m\"}}";
        assertEquals(ProviderStatus.State.FAILED, p.status("pred-1").state());
    }

    @Test
    void decodesBase64Result() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        json = "{\"status\":\"completed\",\"output\":[\"data:image/png;base64,"
                + Base64.getEncoder().encodeToString(png) + "\"]}";
        assertArrayEquals(png, provider().result("pred-1"));
    }

    @Test
    void errorsAreClassified() {
        statusCode = 429;
        assertTrue(assertThrows(ProviderException.class, () -> provider().status("x")).isRetryable());
        statusCode = 400;
        assertFalse(assertThrows(ProviderException.class, () -> provider().status("x")).isRetryable());
    }

    @Test
    void allTypesSupportedAndKeyIsRequired() {
        assertTrue(provider().supports(GarmentType.SAREE));
        assertTrue(provider().supports(GarmentType.LEHENGA));
        assertTrue(provider().supports(GarmentType.SALWAR_SUIT));
        assertThrows(IllegalStateException.class, () -> new FashnTryOnProvider(new TryOnProperties()));
    }
}
