package org.eventa.core.config;

import org.eventa.core.eventstore.EventModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface EventModelRepository extends MongoRepository<EventModel, String> {
    List<EventModel> findByAggregateIdentifier(UUID aggregateIdentifier);
}
