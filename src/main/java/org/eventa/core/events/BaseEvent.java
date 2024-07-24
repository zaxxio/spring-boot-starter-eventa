package org.eventa.core.events;

import lombok.*;
import org.eventa.core.message.Message;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent extends Message {
    private int version;
}
