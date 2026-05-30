package com.company.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "platform.security")
public class SecurityStarterProperties {

    private boolean enabled = true;
    private String requiredAudience;
    private List<String> permitAll = new ArrayList<>(List.of("/actuator/health", "/actuator/info"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRequiredAudience() {
        return requiredAudience;
    }

    public void setRequiredAudience(String requiredAudience) {
        this.requiredAudience = requiredAudience;
    }

    public List<String> getPermitAll() {
        return permitAll;
    }

    public void setPermitAll(List<String> permitAll) {
        this.permitAll = permitAll;
    }
}
