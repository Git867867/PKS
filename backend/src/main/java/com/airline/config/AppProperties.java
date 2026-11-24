package com.airline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Bootstrap bootstrap = new Bootstrap();
    private final Cors cors = new Cors();
    private final Security security = new Security();

    public Jwt getJwt() {
        return jwt;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public Cors getCors() {
        return cors;
    }

    public Security getSecurity() {
        return security;
    }

    public static class Jwt {
        private String secretBase64;
        private String issuer;
        private int accessMinutes;
        private int refreshDays;

        public String getSecretBase64() { return secretBase64; }
        public void setSecretBase64(String secretBase64) { this.secretBase64 = secretBase64; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public int getAccessMinutes() { return accessMinutes; }
        public void setAccessMinutes(int accessMinutes) { this.accessMinutes = accessMinutes; }
        public int getRefreshDays() { return refreshDays; }
        public void setRefreshDays(int refreshDays) { this.refreshDays = refreshDays; }
    }

    public static class Bootstrap {
        private String email;
        private String name;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Cors {
        private String allowedOrigins;

        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Security {
        private boolean cookieSecure;
        private boolean permanentDeleteEnabled;
        private int maxFailedLogins;
        private int lockMinutes;
        private int maxPageSize;

        public boolean isCookieSecure() { return cookieSecure; }
        public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
        public boolean isPermanentDeleteEnabled() { return permanentDeleteEnabled; }
        public void setPermanentDeleteEnabled(boolean permanentDeleteEnabled) { this.permanentDeleteEnabled = permanentDeleteEnabled; }
        public int getMaxFailedLogins() { return maxFailedLogins; }
        public void setMaxFailedLogins(int maxFailedLogins) { this.maxFailedLogins = maxFailedLogins; }
        public int getLockMinutes() { return lockMinutes; }
        public void setLockMinutes(int lockMinutes) { this.lockMinutes = lockMinutes; }
        public int getMaxPageSize() { return maxPageSize; }
        public void setMaxPageSize(int maxPageSize) { this.maxPageSize = maxPageSize; }
    }
}
