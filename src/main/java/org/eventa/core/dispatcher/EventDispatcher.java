package org.eventa.core.dispatcher;

import org.eventa.core.events.BaseEvent;

import java.util.concurrent.CompletableFuture;

public interface EventDispatcher {
    void dispatch(BaseEvent baseEvent);
}
