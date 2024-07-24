package org.eventa.core.commands;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommandMessage<T> {
    private T command;
}
