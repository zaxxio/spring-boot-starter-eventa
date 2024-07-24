package org.eventa.core.aggregate;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.streotype.EventSourcingHandler;
import org.eventa.core.streotype.RoutingKey;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@Log4j2
@NoArgsConstructor
public abstract class AggregateRoot {

    protected UUID aggregateIdentifier;
    private int version = -1;
    private List<BaseEvent> uncommittedChanges = new CopyOnWriteArrayList<>();

    protected void apply(BaseEvent baseEvent) {
        apply(baseEvent, true);
    }

    public void markChangesAsCommitted() {
        this.uncommittedChanges.clear();
    }

    public void replayEvents(List<BaseEvent> baseEvents) {
        for (BaseEvent baseEvent : baseEvents) {
            apply(baseEvent, false);
        }
    }

    private void apply(BaseEvent baseEvent, boolean isNewEvent) {
        this.handleEvent(baseEvent);
        if (isNewEvent) {
            this.uncommittedChanges.add(baseEvent);
        }
        this.version++;
    }

    private void handleEvent(BaseEvent baseEvent) {
        try {
            for (Method method : getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(EventSourcingHandler.class) &&
                        method.getParameterTypes()[0] == baseEvent.getClass()) {
                    try {
                        method.invoke(this, baseEvent);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to handle event", e);
                    }
                    for (Field field : getClass().getDeclaredFields()) {
                        if (field.isAnnotationPresent(RoutingKey.class)) {
                            field.setAccessible(true);
                            this.aggregateIdentifier = (UUID) field.get(this);
                            baseEvent.setMessageId((UUID) field.get(this));
                            break;
                        }
                    }
                    break;
                }
            }

        } catch (Exception e) {
            log.error(e);
        }
    }

}
