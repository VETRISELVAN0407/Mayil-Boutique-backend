package com.mayilboutique.tryon.provider;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.TryOnProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Exercises the real HTTP client against a local stand-in that follows the tryon-service contract. This checks the
 * wiring (paths, headers, multipart fields, error mapping), not the AI.
 */
class SelfHostedTryOnProviderTest {

    private HttpServer server;
    private final AtomicReference<String> apiKey = new AtomicReference<>();
    private final AtomicReference<String> postedBody = new AtomicReference<>();
    private final AtomicReference<String> deleted = new AtomicReference<>();
    private volatile int statusCode = 200;
    private volatile String json = "{\"id\":\"job-1\"}";
    private volatile byte[] imageBytes = {1, 2, 3};

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/jobs", this::handle);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange ex) throws IOException {
        apiKey.set(ex.getRequestHeaders().getFirst("X-Api-Key"));
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        byte[] out;
        String type = "application/json";
        if (method.equals("POST")) {
            postedBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            out = json.getBytes(StandardCharsets.UTF_8);
        } else if (method.equals("DELETE")) {
            deleted.set(path);
            out = new byte[0];
        } else if (path.endsWith("/result")) {
            out = imageBytes;
            type = "image/png";
        } else {
            out = json.getBytes(StandardCharsets.UTF_8);
        }
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(statusCode, statusCode == 204 || out.length == 0 ? -1 : out.length);
        if (out.length > 0 && statusCode != 204) {
            ex.getResponseBody().write(out);
        }
        ex.close();
    }

    private SelfHostedTryOnProvider provider() {
        TryOnProperties props = new TryOnProperties();
        props.setServiceUrl("http://127.0.0.1:" + server.getAddress().getPort());
        props.setServiceApiKey("secret-key");
        props.setServiceTimeoutMs(2000);
        return new SelfHostedTryOnProvider(props);
    }

    private static TryOnRequest request(GarmentType type) {
        return new TryOnRequest(type, new byte[] {9, 9}, List.of(new byte[] {7, 7}));
    }

    @Test
    void refusesToStartWithoutAServiceUrl() {
        assertThrows(IllegalStateException.class, () -> new SelfHostedTryOnProvider(new TryOnProperties()));
    }

    @Test
    void supportsOnlyUpperBodyAndDress() {
        SelfHostedTryOnProvider p = provider();
        assertTrue(p.supports(GarmentType.UPPER_BODY));
        assertTrue(p.supports(GarmentType.DRESS));
        assertFalse(p.supports(GarmentType.SAREE));
        assertFalse(p.supports(GarmentType.LEHENGA));
        assertFalse(p.supports(GarmentType.SALWAR_SUIT));
    }

    @Test
    void startPostsMultipartWithApiKeyAndCategory() throws Exception {
        assertEquals("job-1", provider().start(request(GarmentType.DRESS)));
        assertEquals("secret-key", apiKey.get());
        assertTrue(postedBody.get().contains("name=\"person\""));
        assertTrue(postedBody.get().contains("name=\"garment\""));
        assertTrue(postedBody.get().contains("DRESS"));
    }

    @Test
    void startRejectsUnsupportedTypeWithoutCallingTheService() {
        ProviderException ex = assertThrows(ProviderException.class, () -> provider().start(request(GarmentType.SAREE)));
        assertFalse(ex.isRetryable());
        assertEquals(null, postedBody.get());
    }

    @Test
    void statusMapsServiceValues() throws Exception {
        json = "{\"status\":\"done\"}";
        assertEquals(ProviderStatus.State.DONE, provider().status("job-1").state());
        json = "{\"status\":\"running\"}";
        assertEquals(ProviderStatus.State.RUNNING, provider().status("job-1").state());
        json = "{\"status\":\"failed\"}";
        assertEquals(ProviderStatus.State.FAILED, provider().status("job-1").state());
    }

    @Test
    void resultReturnsTheImageBytes() throws Exception {
        assertArrayEquals(new byte[] {1, 2, 3}, provider().result("job-1"));
    }

    @Test
    void serverErrorsAreRetryableAndClientErrorsAreNot() {
        statusCode = 503;
        assertTrue(assertThrows(ProviderException.class, () -> provider().status("job-1")).isRetryable());
        statusCode = 404;
        assertFalse(assertThrows(ProviderException.class, () -> provider().status("job-1")).isRetryable());
        statusCode = 422;
        assertFalse(assertThrows(ProviderException.class, () -> provider().start(request(GarmentType.DRESS))).isRetryable());
    }

    @Test
    void unreachableServiceIsRetryable() {
        SelfHostedTryOnProvider p = provider();
        server.stop(0);
        assertTrue(assertThrows(ProviderException.class, () -> p.status("job-1")).isRetryable());
    }

    @Test
    void deleteCallsTheServiceAndNeverThrows() {
        provider().delete("job-1");
        assertEquals("/v1/jobs/job-1", deleted.get());
        statusCode = 500;
        provider().delete("job-1"); // logged, not thrown
        server.stop(0);
        provider().delete("job-1");
    }
}
