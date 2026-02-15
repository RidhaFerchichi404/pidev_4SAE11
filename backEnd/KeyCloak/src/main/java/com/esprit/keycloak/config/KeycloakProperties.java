package com.esprit.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String authServerUrl = "http://localhost:8180";
    private String realm = "smart-freelance";
    private String resource = "smart-freelance-backend";
    /** Secret for server-to-server calls (e.g. User service sync delete/update). Set same value in User service. */
    private String serviceSecret = "";
    private Credentials credentials = new Credentials();
    private Admin admin = new Admin();

    public String getServiceSecret() {
        return serviceSecret != null ? serviceSecret : "";
    }

    public void setServiceSecret(String serviceSecret) {
        this.serviceSecret = serviceSecret;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public static class Credentials {
        private String secret = "";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public static class Admin {
        private String realm = "master";
        private String clientId = "admin-cli";
        private String clientSecret = "";
        private String username = "admin";
        private String password = "admin";

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret != null ? clientSecret : "";
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
