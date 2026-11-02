package com.mayilboutique.tryon.provider;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.TryOnProperties;

/**
 * FASHN's hosted API (fashn.ai, model {@code tryon-max}). Customer photos are sent to FASHN, so the consent and
 * privacy wording must say so. Images go in as data URIs and come back as base64 ({@code return_base64}), which
 * FASHN keeps for 60 minutes instead of the 3 days of a CDN URL. FASHN has no delete endpoint, so {@link #delete}
 * is a no-op. All garment types are enabled, including saree, lehenga and salwar suit, which FASHN does not document:
 * quality for those must be checked with real previews before launch.
 */
@Component
@ConditionalOnProperty(prefix = "app.tryon", name = "provider", havingValue = "fashn")
public class FashnTryOnProvider implements VirtualTryOnProvider {

    private static final Logger log = LoggerFactory.getLogger(FashnTryOnProvider.class);
    private static final String DEFAULT_URL = "https://api.fashn.ai";
    private static final Set<GarmentType> SUPPORTED = Set.of(GarmentType.values());

    private final RestClient client;

    public FashnTryOnProvider(TryOnProperties props) {
        if (props.getServiceApiKey() == null || props.getServiceApiKey().isBlank()) {
            throw new IllegalStateException("app.tryon.provider=fashn requires TRYON_SERVICE_API_KEY (your FASHN API key)");
        }
        String url = props.getServiceUrl() == null || props.getServiceUrl().isBlank() ? DEFAULT_URL : props.getServiceUrl();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getServiceTimeoutMs());
        factory.setReadTimeout(props.getServiceTimeoutMs());
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + props.getServiceApiKey()).build();
    }

    @Override
    public String name() {
        return "fashn";
    }

    @Override
    public boolean supports(GarmentType type) {
        return SUPPORTED.contains(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String start(TryOnRequest request) throws ProviderException {
        if (!supports(request.garmentType()) || request.garments().isEmpty()) {
            throw new ProviderException("Unsupported garment type or no garment image", false);
        }
        Map<String, Object> body = Map.of(
                "model_name", "tryon-max",
                "inputs", Map.of(
                        "model_image", dataUri(request.personPhoto()),
                        "product_image", dataUri(request.garments().get(0)),
                        "resolution", "1k",
                        "output_format", "png",
                        "return_base64", true));
        Map<String, Object> response = call(() -> client.post().uri("/v1/run")
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class));
        Object id = response == null ? null : response.get("id");
        if (!(id instanceof String s) || s.isBlank()) {
            throw new ProviderException("Provider did not return a job id", true);
        }
        return s;
    }

    @Override
    public ProviderStatus status(String providerJobId) throws ProviderException {
        Map<String, Object> response = fetch(providerJobId);
        Object state = response == null ? null : response.get("status");
        if (state == null) {
            throw new ProviderException("Provider returned no status", true);
        }
        return switch (state.toString()) {
            case "starting", "in_queue" -> new ProviderStatus(ProviderStatus.State.QUEUED);
            case "processing" -> new ProviderStatus(ProviderStatus.State.RUNNING);
            case "completed" -> new ProviderStatus(ProviderStatus.State.DONE);
            case "failed" -> new ProviderStatus(ProviderStatus.State.FAILED);
            default -> throw new ProviderException("Unknown provider status", true);
        };
    }

    @Override
    public byte[] result(String providerJobId) throws ProviderException {
        Map<String, Object> response = fetch(providerJobId);
        Object output = response == null ? null : response.get("output");
        if (!(output instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof String data)) {
            throw new ProviderException("Provider returned no result", true);
        }
        int comma = data.indexOf(',');
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(data.startsWith("data:") && comma > 0 ? data.substring(comma + 1) : data);
        } catch (IllegalArgumentException e) {
            throw new ProviderException("Provider result was not valid base64", true);
        }
        if (bytes.length == 0) {
            throw new ProviderException("Provider returned an empty result", true);
        }
        return bytes;
    }

    /** FASHN offers no deletion endpoint; base64 results expire on their own after 60 minutes. */
    @Override
    public void delete(String providerJobId) {
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetch(String id) throws ProviderException {
        return call(() -> client.get().uri("/v1/status/{id}", id).retrieve().body(Map.class));
    }

    private interface Call<T> {
        T run();
    }

    /** Maps HTTP/network errors to {@link ProviderException}; only status codes are logged, never URLs or bodies. */
    private <T> T call(Call<T> call) throws ProviderException {
        try {
            return call.run();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.error("FASHN returned HTTP {}", status);
            boolean retryable = status >= 500 || status == 429 || status == 408;
            throw new ProviderException("FASHN answered HTTP " + status, retryable);
        } catch (RestClientException e) {
            log.error("FASHN request failed ({})", e.getClass().getSimpleName());
            throw new ProviderException("FASHN unreachable", true);
        }
    }

    private static String dataUri(byte[] image) {
        String mime = image.length > 3 && (image[0] & 0xFF) == 0x89 ? "image/png"
                : image.length > 3 && image[0] == 'R' ? "image/webp" : "image/jpeg";
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(image);
    }
}
