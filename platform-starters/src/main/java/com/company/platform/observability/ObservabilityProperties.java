package com.company.platform.observability;

import java.time.Duration;

/**
 * Configuration properties for observability features.
 * 
 * Prefix: platform.observability
 */
public class ObservabilityProperties {
    
    private Tracing tracing = new Tracing();
    private Metrics metrics = new Metrics();
    private Health health = new Health();
    
    public Tracing getTracing() {
        return tracing;
    }
    
    public void setTracing(Tracing tracing) {
        this.tracing = tracing;
    }
    
    public Metrics getMetrics() {
        return metrics;
    }
    
    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }
    
    public Health getHealth() {
        return health;
    }
    
    public void setHealth(Health health) {
        this.health = health;
    }
    
    /**
     * Distributed tracing configuration.
     */
    public static class Tracing {
        private boolean enabled = true;
        private double samplingRate = 1.0; // 100% sampling by default (reduce in production)
        private String serviceName;
        private String environment;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public double getSamplingRate() {
            return samplingRate;
        }
        
        public void setSamplingRate(double samplingRate) {
            this.samplingRate = samplingRate;
        }
        
        public String getServiceName() {
            return serviceName;
        }
        
        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }
        
        public String getEnvironment() {
            return environment;
        }
        
        public void setEnvironment(String environment) {
            this.environment = environment;
        }
    }
    
    /**
     * Metrics configuration.
     */
    public static class Metrics {
        private boolean enabled = true;
        private boolean jvmMetricsEnabled = true;
        private boolean httpMetricsEnabled = true;
        private boolean dbMetricsEnabled = true;
        private boolean eventingMetricsEnabled = true;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isJvmMetricsEnabled() {
            return jvmMetricsEnabled;
        }
        
        public void setJvmMetricsEnabled(boolean jvmMetricsEnabled) {
            this.jvmMetricsEnabled = jvmMetricsEnabled;
        }
        
        public boolean isHttpMetricsEnabled() {
            return httpMetricsEnabled;
        }
        
        public void setHttpMetricsEnabled(boolean httpMetricsEnabled) {
            this.httpMetricsEnabled = httpMetricsEnabled;
        }
        
        public boolean isDbMetricsEnabled() {
            return dbMetricsEnabled;
        }
        
        public void setDbMetricsEnabled(boolean dbMetricsEnabled) {
            this.dbMetricsEnabled = dbMetricsEnabled;
        }
        
        public boolean isEventingMetricsEnabled() {
            return eventingMetricsEnabled;
        }
        
        public void setEventingMetricsEnabled(boolean eventingMetricsEnabled) {
            this.eventingMetricsEnabled = eventingMetricsEnabled;
        }
    }
    
    /**
     * Health check configuration.
     */
    public static class Health {
        private Duration timeout = Duration.ofSeconds(5);
        private boolean dbCheckEnabled = true;
        private boolean eventingCheckEnabled = true;
        
        public Duration getTimeout() {
            return timeout;
        }
        
        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
        
        public boolean isDbCheckEnabled() {
            return dbCheckEnabled;
        }
        
        public void setDbCheckEnabled(boolean dbCheckEnabled) {
            this.dbCheckEnabled = dbCheckEnabled;
        }
        
        public boolean isEventingCheckEnabled() {
            return eventingCheckEnabled;
        }
        
        public void setEventingCheckEnabled(boolean eventingCheckEnabled) {
            this.eventingCheckEnabled = eventingCheckEnabled;
        }
    }
}
