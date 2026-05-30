package com.company.platform.logging;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * TaskDecorator that propagates MDC (Mapped Diagnostic Context) to async tasks.
 * 
 * This ensures that correlation IDs, trace IDs, and other MDC fields are available
 * in logs produced by @Async methods and other asynchronous operations.
 * 
 * Without this decorator, MDC context is lost when crossing thread boundaries,
 * making it impossible to correlate logs from async operations with their originating request.
 * 
 * Usage:
 * <pre>
 * {@code
 * @Configuration
 * public class AsyncConfig {
 *     @Bean
 *     public Executor taskExecutor() {
 *         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *         executor.setTaskDecorator(new MdcTaskDecorator());
 *         return executor;
 *     }
 * }
 * }
 * </pre>
 */
public class MdcTaskDecorator implements TaskDecorator {
    
    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        // Capture MDC context from the calling thread
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        
        return () -> {
            try {
                // Restore MDC context in the executing thread
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                
                // Execute the actual task
                runnable.run();
                
            } finally {
                // Clean up MDC to prevent memory leaks
                MDC.clear();
            }
        };
    }
}
