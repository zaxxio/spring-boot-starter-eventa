package org.eventa.core.eventstore;

import lombok.Builder;
import lombok.Data;
import org.eventa.core.events.BaseEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@Document(collection = "events")
public class EventModel {
    @Id
    private String id;
    @Indexed
    private UUID aggregateIdentifier;
    private String aggregateType;
    private String eventType;
    private BaseEvent baseEvent;
    private int version;
    @Indexed
    private Date timestamp;
}
