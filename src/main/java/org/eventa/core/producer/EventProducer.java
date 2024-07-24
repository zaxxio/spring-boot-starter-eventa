package org.eventa.core.producer;

import org.eventa.core.events.BaseEvent;

public interface EventProducer {
    String produceEvent(String topic, BaseEvent baseEvent) throws Exception;
}
