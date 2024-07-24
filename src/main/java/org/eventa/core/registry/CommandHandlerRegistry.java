package org.eventa.core.registry;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Component
public class CommandHandlerRegistry {

    private final Map<Class<?>, List<CommandHandlerType>> commandHandlers = new ConcurrentHashMap<>();

    public void registerHandler(Class<?> type, Method method) {
        this.commandHandlers.computeIfAbsent(type, commands -> Collections.singletonList(new CommandHandlerType(CommandHandlerType.HandlerType.METHOD, method)));
    }

    public void registerConstructorHandler(Class<?> type, Constructor<?> constructor) {
        this.commandHandlers.computeIfAbsent(type, commands -> Collections.singletonList(new CommandHandlerType(CommandHandlerType.HandlerType.CONSTRUCTOR, constructor)));
    }

    public CommandHandlerType getHandler(Class<?> commandType) {
        List<CommandHandlerType> commandHandlerTypes = this.commandHandlers.get(commandType);
        if (commandHandlerTypes == null) {
            throw new RuntimeException(String.format("%s Command Handler is not found.", commandType.getName()));
        }
        if (commandHandlerTypes.size() > 1) {
            throw new RuntimeException("More than one Command handler is registered");
        }
        return commandHandlerTypes.get(0);
    }
}
