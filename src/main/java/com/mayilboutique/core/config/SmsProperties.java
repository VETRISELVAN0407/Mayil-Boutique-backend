package com.mayilboutique.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** SMS delivery settings. {@code provider} is one of: none (default), msg91, log (local dev only), dummy (test mode, never in prod). */
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    private String provider = "none";
    private final Msg91 msg91 = new Msg91();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Msg91 getMsg91() {
        return msg91;
    }

    public static class Msg91 {

        private String baseUrl = "https://control.msg91.com";
        private String authKey = "";
        private String templateId = "";
        private String countryCode = "91";
        private int timeoutMs = 8000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getAuthKey() {
            return authKey;
        }

        public void setAuthKey(String authKey) {
            this.authKey = authKey;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
