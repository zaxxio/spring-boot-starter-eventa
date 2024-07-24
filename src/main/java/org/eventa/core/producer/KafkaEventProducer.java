package org.eventa.core.producer;

import lombok.extern.log4j.Log4j2;
import org.eventa.core.events.BaseEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Log4j2
@Component
public class KafkaEventProducer implements EventProducer {

    @Autowired
    private KafkaTemplate<UUID, Object> kafkaTemplate;

    @Override
    // @Transactional(transactionManager = "kafkaTransactionManager", rollbackFor = Exception.class)
    public String produceEvent(String topic, BaseEvent baseEvent) {
        final Message<?> message = MessageBuilder
                .withPayload(baseEvent)
                .setHeader(KafkaHeaders.KEY, UUID.randomUUID())
                .setHeader("messageId", baseEvent.getMessageId())
                .setHeader(KafkaHeaders.CORRELATION_ID, UUID.randomUUID())
                .setHeader("schema.version", "v1")
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.TIMESTAMP, System.currentTimeMillis())
                .build();
        SendResult<UUID, Object> persistedMessage = kafkaTemplate.send(message).join();
        return persistedMessage.getProducerRecord().key().toString();
    }
}
