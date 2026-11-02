package com.mayilboutique.tryon.provider;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.TryOnProperties;

/**
 * Talks to our own open-source try-on service ({@code tryon-service/}, FASHN VTON v1.5). Its model handles tops and
 * dresses only, so sarees, lehengas and salwar suits are reported as unsupported rather than approximated.
 */
@Component
@ConditionalOnProperty(prefix = "app.tryon", name = "provider", havingValue = "selfhosted")
public class SelfHostedTryOnProvider implements VirtualTryOnProvider {

    private static final Logger log = LoggerFactory.getLogger(SelfHostedTryOnProvider.class);
    private static final Set<GarmentType> SUPPORTED = Set.of(GarmentType.UPPER_BODY, GarmentType.DRESS);

    private final RestClient client;

    public SelfHostedTryOnProvider(TryOnProperties props) {
        if (props.getServiceUrl() == null || props.getServiceUrl().isBlank()) {
            throw new IllegalStateException("app.tryon.provider=selfhosted requires TRYON_SERVICE_URL to be set");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getServiceTimeoutMs());
        factory.setReadTimeout(props.getServiceTimeoutMs());
        RestClient.Builder builder = RestClient.builder().baseUrl(props.getServiceUrl()).requestFactory(factory);
        if (props.getServiceApiKey() != null && !props.getServiceApiKey().isBlank()) {
            builder.defaultHeader("X-Api-Key", props.getServiceApiKey());
        }
        this.client = builder.build();
    }

    @Override
    public String name() {
        return "selfhosted";
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
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("person", named(request.personPhoto(), "person"));
        body.add("garment", named(request.garments().get(0), "garment")); // this engine uses one garment image
        body.add("category", request.garmentType().name());
        Map<String, Object> response = call(() -> client.post().uri("/v1/jobs")
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(Map.class));
        Object id = response == null ? null : response.get("id");
        if (!(id instanceof String s) || s.isBlank()) {
            throw new ProviderException("Provider did not return a job id", true);
        }
        return s;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ProviderStatus status(String providerJobId) throws ProviderException {
        Map<String, Object> response = call(() -> client.get().uri("/v1/jobs/{id}", providerJobId)
                .retrieve().body(Map.class));
        Object state = response == null ? null : response.get("status");
        if (state == null) {
            throw new ProviderException("Provider returned no status", true);
        }
        return switch (state.toString()) {
            case "queued" -> new ProviderStatus(ProviderStatus.State.QUEUED);
            case "running" -> new ProviderStatus(ProviderStatus.State.RUNNING);
            case "done" -> new ProviderStatus(ProviderStatus.State.DONE);
            case "failed" -> new ProviderStatus(ProviderStatus.State.FAILED);
            default -> throw new ProviderException("Unknown provider status", true);
        };
    }

    @Override
    public byte[] result(String providerJobId) throws ProviderException {
        byte[] data = call(() -> client.get().uri("/v1/jobs/{id}/result", providerJobId).retrieve().body(byte[].class));
        if (data == null || data.length == 0) {
            throw new ProviderException("Provider returned an empty result", true);
        }
        return data;
    }

    @Override
    public void delete(String providerJobId) {
        try {
            client.delete().uri("/v1/jobs/{id}", providerJobId).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != 404) {
                log.warn("Try-on service delete returned HTTP {}", e.getStatusCode().value());
            }
        } catch (RestClientException e) {
            log.warn("Try-on service delete failed ({})", e.getClass().getSimpleName());
        }
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
            log.error("Try-on service returned HTTP {}", status);
            boolean retryable = status >= 500 || status == 429 || status == 408;
            throw new ProviderException("Try-on service answered HTTP " + status, retryable);
        } catch (RestClientException e) {
            log.error("Try-on service request failed ({})", e.getClass().getSimpleName());
            throw new ProviderException("Try-on service unreachable", true);
        }
    }

    private static ByteArrayResource named(byte[] data, String name) {
        return new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }
}
