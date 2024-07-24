package org.eventa.core.eventstore;

import org.eventa.core.events.BaseEvent;

import java.util.List;
import java.util.UUID;

public interface EventStore {
    String saveEvents(UUID aggregateIdentifier, List<BaseEvent> baseEvents, int expectedVersion, String aggregateType, boolean isNew) throws InterruptedException;

    List<BaseEvent> getEvents(UUID aggregateIdentifier);

    List<BaseEvent> getEvents();
}
