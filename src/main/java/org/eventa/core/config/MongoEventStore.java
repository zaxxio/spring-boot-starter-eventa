package org.eventa.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.eventstore.EventModel;
import org.eventa.core.eventstore.EventStore;
import org.eventa.core.producer.EventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;


@Log4j2
@RequiredArgsConstructor
public class MongoEventStore implements EventStore {

    @Autowired
    private EventModelRepository eventModelRepository;
    @Autowired
    private EventProducer eventProducer;
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    @Override
    public String saveEvents(UUID aggregateIdentifier, List<BaseEvent> baseEvents, int expectedVersion, String aggregateType, boolean isNew) throws InterruptedException {
        try {
            var eventStream = eventModelRepository.findByAggregateIdentifier(aggregateIdentifier);
            if (!isNew) {
                if (eventStream.stream().map(EventModel::getVersion).max(Comparator.naturalOrder()).get() != expectedVersion) {
                    throw new ConcurrencyFailureException("");
                }
            } else {
                if (!eventStream.isEmpty()) {
                    throw new RuntimeException("Aggregate with id " + aggregateIdentifier + " exists.");
                }
            }
            var version = expectedVersion;
            for (var event : baseEvents) {
                version++;
                event.setVersion(version);
                var eventModel = EventModel.builder()
                        .timestamp(new Date())
                        .aggregateIdentifier(aggregateIdentifier)
                        .aggregateType(aggregateType)
                        .version(version)
                        .eventType(event.getClass().getTypeName())
                        .baseEvent(event)
                        .build();
                var persistedEvent = eventModelRepository.save(eventModel);
                if (!persistedEvent.getId().isEmpty()) {
                    return eventProducer.produceEvent("BaseEvent", event);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return "";
    }

    @Override
    public List<BaseEvent> getEvents(UUID aggregateIdentifier) {
        List<EventModel> eventStream = eventModelRepository.findByAggregateIdentifier(aggregateIdentifier);
        if (eventStream.isEmpty()) {
            throw new RuntimeException("Aggregate " + aggregateIdentifier + " not found");
        }
        return eventStream.stream().map(EventModel::getBaseEvent).collect(Collectors.toList());
    }

    @Override
    public List<BaseEvent> getEvents() {
        List<BaseEvent> eventStream = this.eventModelRepository.findAll().stream().map(EventModel::getBaseEvent).toList();
        if (eventStream.isEmpty()){
            log.error("Could not retrieve events from the events store.'");
        }
        return eventStream;
    }
}
