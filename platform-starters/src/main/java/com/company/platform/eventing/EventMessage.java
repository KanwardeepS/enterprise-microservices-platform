package com.company.platform.eventing;

import java.util.Map;

public record EventMessage(
        String destination,
        String key,
        String payload,
        Map<String, Object> headers
) {
    public EventMessage {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
