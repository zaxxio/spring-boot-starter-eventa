package org.eventa.core.dispatcher;

import org.eventa.core.commands.BaseCommand;
import org.eventa.core.commands.CommandMessage;
import org.eventa.core.commands.CommandResultMessage;

import java.util.UUID;
import java.util.function.BiConsumer;

public interface CommandDispatcher {

    <T extends BaseCommand> void dispatch(T baseCommand, BiConsumer<CommandMessage<T>, CommandResultMessage<?>> callback) throws Exception;
    void acknowledgeCommand(UUID commandId);
}
