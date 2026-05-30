package com.company.platform.eventing;

public interface EventBus {
    void publish(EventMessage message);
}
