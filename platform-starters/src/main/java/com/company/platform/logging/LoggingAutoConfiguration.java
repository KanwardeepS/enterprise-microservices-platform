package com.company.platform.logging;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Auto-configuration for platform logging capabilities.
 * 
 * Provides:
 * - Correlation ID filter for request tracking
 * - MDC-aware async task executor for correlation propagation
 * - Request/response logging filter (optional)
 */
@AutoConfiguration
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
    
    /**
     * Optional request/response logging filter.
     * Disabled by default to avoid performance impact in production.
     * Enable with: platform.observability.logging.request-logging-enabled=true
     */
    @Bean
    @ConditionalOnProperty(
        name = "platform.observability.logging.request-logging-enabled",
        havingValue = "true",
        matchIfMissing = false
    )
    @ConditionalOnMissingBean
    public RequestLoggingFilter requestLoggingFilter(LoggingProperties properties) {
        return new RequestLoggingFilter(
            properties.isRequestLoggingIncludeHeaders(),
            properties.isRequestLoggingIncludePayload(),
            properties.getRequestLoggingExcludedPaths()
        );
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "platform.observability.logging")
    public LoggingProperties loggingProperties() {
        return new LoggingProperties();
    }
    
    /**
     * Configuration for async execution with MDC propagation.
     * Only activated when @EnableAsync is present in the application.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    public static class AsyncExecutorConfiguration implements AsyncConfigurer {
        
        @Override
        public Executor getAsyncExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            
            // Thread pool configuration
            executor.setCorePoolSize(10);
            executor.setMaxPoolSize(50);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("async-");
            executor.setWaitForTasksToCompleteOnShutdown(true);
            executor.setAwaitTerminationSeconds(60);
            
            // CRITICAL: Decorate tasks with MDC propagation
            executor.setTaskDecorator(new MdcTaskDecorator());
            
            executor.initialize();
            return executor;
        }
        
        @Override
        public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return (throwable, method, params) -> {
                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                org.slf4j.LoggerFactory.getLogger(AsyncExecutorConfiguration.class)
                    .error("Uncaught exception in async method: {}", methodName, throwable);
            };
        }
    }
    
    /**
     * Configuration properties for logging features.
     */
    public static class LoggingProperties {
        private boolean requestLoggingEnabled = false;
        private boolean requestLoggingIncludeHeaders = false;
        private boolean requestLoggingIncludePayload = false;
        private Set<String> requestLoggingExcludedPaths = Set.of("/actuator/health", "/actuator/prometheus");
        
        public boolean isRequestLoggingEnabled() {
            return requestLoggingEnabled;
        }
        
        public void setRequestLoggingEnabled(boolean requestLoggingEnabled) {
            this.requestLoggingEnabled = requestLoggingEnabled;
        }
        
        public boolean isRequestLoggingIncludeHeaders() {
            return requestLoggingIncludeHeaders;
        }
        
        public void setRequestLoggingIncludeHeaders(boolean requestLoggingIncludeHeaders) {
            this.requestLoggingIncludeHeaders = requestLoggingIncludeHeaders;
        }
        
        public boolean isRequestLoggingIncludePayload() {
            return requestLoggingIncludePayload;
        }
        
        public void setRequestLoggingIncludePayload(boolean requestLoggingIncludePayload) {
            this.requestLoggingIncludePayload = requestLoggingIncludePayload;
        }
        
        public Set<String> getRequestLoggingExcludedPaths() {
            return requestLoggingExcludedPaths;
        }
        
        public void setRequestLoggingExcludedPaths(Set<String> requestLoggingExcludedPaths) {
            this.requestLoggingExcludedPaths = requestLoggingExcludedPaths;
        }
    }
}


