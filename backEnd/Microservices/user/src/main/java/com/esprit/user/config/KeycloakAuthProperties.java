package com.esprit.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "keycloak.auth")
public class KeycloakAuthProperties {

    /** Base URL of keycloak-auth service (e.g. http://localhost:8079). If blank, Keycloak sync is skipped. */
    private String serviceUrl = "";
    /** Must match keycloak.service-secret in keycloak-auth. */
    private String serviceSecret = "";

    public String getServiceUrl() {
        return serviceUrl != null ? serviceUrl.trim() : "";
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getServiceSecret() {
        return serviceSecret != null ? serviceSecret : "";
    }

    public void setServiceSecret(String serviceSecret) {
        this.serviceSecret = serviceSecret;
    }

    public boolean isConfigured() {
        return !getServiceUrl().isBlank() && !getServiceSecret().isBlank();
    }
}
