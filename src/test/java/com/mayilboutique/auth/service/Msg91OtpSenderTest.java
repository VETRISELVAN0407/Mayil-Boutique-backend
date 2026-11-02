package com.mayilboutique.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.mayilboutique.core.config.SmsProperties;
import com.mayilboutique.core.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;

/** Exercises the real HTTP client against a local stand-in for the MSG91 endpoint. */
class Msg91OtpSenderTest {

    private HttpServer server;
    private final AtomicReference<String> requestUri = new AtomicReference<>();
    private final AtomicReference<String> authKeyHeader = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String body = "{\"type\":\"success\",\"message\":\"req-id\"}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v5/otp", exchange -> {
            requestUri.set(exchange.getRequestURI().toString());
            authKeyHeader.set(exchange.getRequestHeaders().getFirst("authkey"));
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private Msg91OtpSender sender(String authKey, String templateId) {
        SmsProperties.Msg91 config = new SmsProperties.Msg91();
        config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        config.setAuthKey(authKey);
        config.setTemplateId(templateId);
        config.setTimeoutMs(2000);
        return new Msg91OtpSender(config);
    }

    @Test
    void sendsOtpToMsg91WithCountryCodeTemplateAndAuthKeyHeader() {
        sender("key-123", "tmpl-9").send("9876543210", "482913");

        String uri = requestUri.get();
        assertTrue(uri.contains("template_id=tmpl-9"), uri);
        assertTrue(uri.contains("mobile=919876543210"), uri);
        assertTrue(uri.contains("otp=482913"), uri);
        assertEquals("key-123", authKeyHeader.get());
        assertTrue(!uri.contains("key-123"), "auth key must not travel in the URL");
    }

    @Test
    void msg91ErrorBodyOnHttp200IsTreatedAsDeliveryFailure() {
        body = "{\"type\":\"error\",\"message\":\"Template not found\"}";

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sender("k", "t").send("9876543210", "123456"));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getHttpStatus());
        assertTrue(!ex.getMessage().contains("Template"), "provider details must not leak to the client");
    }

    @Test
    void httpErrorIsTreatedAsDeliveryFailure() {
        status = 401;
        body = "{\"type\":\"error\",\"message\":\"Invalid authkey\"}";

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sender("bad", "t").send("9876543210", "123456"));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getHttpStatus());
    }

    @Test
    void unreachableProviderIsTreatedAsDeliveryFailure() {
        Msg91OtpSender sender = sender("k", "t");
        server.stop(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> sender.send("9876543210", "123456"));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getHttpStatus());
    }

    @Test
    void refusesToStartWithoutCredentials() {
        assertThrows(IllegalStateException.class, () -> sender("", "t"));
        assertThrows(IllegalStateException.class, () -> sender("k", " "));
    }
}
