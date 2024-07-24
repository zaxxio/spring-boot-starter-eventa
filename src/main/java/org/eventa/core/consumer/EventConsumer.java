package org.eventa.core.consumer;

import org.eventa.core.events.BaseEvent;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

public interface EventConsumer extends AcknowledgingMessageListener<UUID, BaseEvent> {
    void consume(BaseEvent baseEvent, Acknowledgment acknowledgment) throws Exception;
}
