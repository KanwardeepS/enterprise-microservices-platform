package com.company.platform.eventing;

public interface EventMessageHandler {
    void onMessage(EventMessage message);
}
