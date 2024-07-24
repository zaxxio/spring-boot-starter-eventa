package org.eventa.core.consumer.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eventa.core.consumer.EventConsumer;
import org.eventa.core.dispatcher.EventDispatcher;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.registry.EventHandlerRegistry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Log4j2
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer implements EventConsumer {

    private final EventDispatcher eventDispatcher;
    private final EventHandlerRegistry eventHandlerRegistry;

    @Override
    public void consume(@Payload BaseEvent baseEvent, Acknowledgment acknowledgment) throws Exception {
        log.debug("Received : {}", baseEvent);
        if (eventHandlerRegistry.isRegistered(baseEvent.getClass())) {
            try {
                eventDispatcher.dispatch(baseEvent);
                log.debug("Processed Event : {}", baseEvent);
                acknowledgment.acknowledge();
            } catch (Exception ex) {
                log.debug("Error Processing Event : {}, Error Message : {}", baseEvent, ex.getMessage());
            }
        }
    }

    @Override
    @KafkaListener(topics = "${eventa.event-bus: BaseEvent}")
    public void onMessage(ConsumerRecord<UUID, BaseEvent> consumerRecord, Acknowledgment acknowledgment) {
        try {
            consume(consumerRecord.value(), acknowledgment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
