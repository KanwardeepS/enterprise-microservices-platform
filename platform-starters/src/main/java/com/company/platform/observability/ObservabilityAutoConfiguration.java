package com.company.platform.observability;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for observability features including metrics, tracing, and health checks.
 * 
 * Provides:
 * - Micrometer metrics with common tags
 * - JVM metrics (memory, GC, threads)
 * - @Timed annotation support
 * - Custom health indicators (via component scan)
 * - Configuration properties for all observability features
 */
@AutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackageClasses = ObservabilityAutoConfiguration.class)
public class ObservabilityAutoConfiguration {
    
    @Bean
    @ConfigurationProperties(prefix = "platform.observability")
    public ObservabilityProperties observabilityProperties() {
        return new ObservabilityProperties();
    }
    
    /**
     * Customize meter registry with common tags (service name, environment, version).
     * These tags are added to all metrics for easier filtering and aggregation.
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(ObservabilityProperties properties) {
        return registry -> {
            // Add service metadata tags
            if (properties.getTracing().getServiceName() != null) {
                registry.config().commonTags("service", properties.getTracing().getServiceName());
            }
            
            if (properties.getTracing().getEnvironment() != null) {
                registry.config().commonTags("environment", properties.getTracing().getEnvironment());
            }
            
            // Add application version if available (from manifest or build info)
            String version = getClass().getPackage().getImplementationVersion();
            if (version != null) {
                registry.config().commonTags("version", version);
            }
        };
    }
    
    /**
     * Enable @Timed annotation support for method-level timing metrics.
     */
    @Bean
    @ConditionalOnClass(TimedAspect.class)
    @ConditionalOnProperty(name = "platform.observability.metrics.enabled", havingValue = "true", matchIfMissing = true)
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    /**
     * JVM memory metrics (heap, non-heap, buffer pools).
     */
    @Bean
    @ConditionalOnClass(JvmMemoryMetrics.class)
    @ConditionalOnProperty(name = "platform.observability.metrics.jvm-metrics-enabled", havingValue = "true", matchIfMissing = true)
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }
    
    /**
     * JVM garbage collection metrics.
     */
    @Bean
    @ConditionalOnClass(JvmGcMetrics.class)
    @ConditionalOnProperty(name = "platform.observability.metrics.jvm-metrics-enabled", havingValue = "true", matchIfMissing = true)
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }
    
    /**
     * JVM thread metrics (live, peak, daemon, states).
     */
    @Bean
    @ConditionalOnClass(JvmThreadMetrics.class)
    @ConditionalOnProperty(name = "platform.observability.metrics.jvm-metrics-enabled", havingValue = "true", matchIfMissing = true)
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }
    
    /**
     * System processor metrics (CPU usage, load average).
     */
    @Bean
    @ConditionalOnClass(ProcessorMetrics.class)
    @ConditionalOnProperty(name = "platform.observability.metrics.jvm-metrics-enabled", havingValue = "true", matchIfMissing = true)
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }
}
