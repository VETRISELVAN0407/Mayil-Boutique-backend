package com.mayilboutique.auth.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.mayilboutique.core.config.SmsProperties;
import com.mayilboutique.core.exception.BusinessException;

/**
 * Delivers OTPs through the MSG91 OTP API. The code is generated and verified by this application;
 * MSG91 only delivers it using the DLT-approved template referenced by {@code templateId}.
 */
public class Msg91OtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(Msg91OtpSender.class);
    private static final int OTP_EXPIRY_MINUTES = 5;
    static final String DELIVERY_FAILED = "We could not send the OTP right now. Please try again shortly.";

    private final SmsProperties.Msg91 config;
    private final RestClient client;

    public Msg91OtpSender(SmsProperties.Msg91 config) {
        if (isBlank(config.getAuthKey()) || isBlank(config.getTemplateId())) {
            throw new IllegalStateException(
                    "app.sms.provider=msg91 requires MSG91_AUTH_KEY and MSG91_TEMPLATE_ID to be set");
        }
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeoutMs());
        factory.setReadTimeout(config.getTimeoutMs());
        this.client = RestClient.builder().baseUrl(config.getBaseUrl()).requestFactory(factory).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void send(String mobile, String code) {
        Map<String, Object> response;
        try {
            response = client.post()
                    .uri(uri -> uri.path("/api/v5/otp")
                            .queryParam("template_id", config.getTemplateId())
                            .queryParam("mobile", config.getCountryCode() + mobile)
                            .queryParam("otp", code)
                            .queryParam("otp_expiry", OTP_EXPIRY_MINUTES)
                            .build())
                    .header("authkey", config.getAuthKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            // 401/403 usually means a wrong or IP-restricted auth key. Status only: never the URL, headers or body.
            log.error("MSG91 returned HTTP {} for mobile ending {}", e.getStatusCode().value(), tail(mobile));
            throw new BusinessException(HttpStatus.BAD_GATEWAY, DELIVERY_FAILED);
        } catch (RestClientException e) {
            // Never log the URL/headers: they carry the OTP and the auth key.
            log.error("MSG91 request failed for mobile ending {}: {}", tail(mobile), e.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, DELIVERY_FAILED);
        }

        Object type = response == null ? null : response.get("type");
        if (!"success".equals(type)) {
            // MSG91 returns HTTP 200 with type=error for bad key/template/DLT problems.
            log.error("MSG91 rejected OTP for mobile ending {}: type={}, message={}",
                    tail(mobile), type, response == null ? null : response.get("message"));
            throw new BusinessException(HttpStatus.BAD_GATEWAY, DELIVERY_FAILED);
        }
        log.info("OTP handed to MSG91 for mobile ending {}", tail(mobile));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String tail(String mobile) {
        return mobile.length() > 4 ? mobile.substring(mobile.length() - 4) : "****";
    }
}
