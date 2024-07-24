package org.eventa.core.registry;

import lombok.Data;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@Data
public class CommandHandlerType {

    private Method commandHandlerMethod;
    private Constructor<?> commandHandlerConstructor;
    private HandlerType handlerType;

    public CommandHandlerType(HandlerType handlerType, Method commandHandlerMethod) {
        this.handlerType = handlerType;
        this.commandHandlerMethod = commandHandlerMethod;
    }

    public CommandHandlerType(HandlerType handlerType, Constructor<?> commandHandlerConstructor) {
        this.handlerType = handlerType;
        this.commandHandlerConstructor = commandHandlerConstructor;
    }

    public HandlerType handlerType() {
        return handlerType;
    }

    public enum HandlerType {
        CONSTRUCTOR, METHOD
    }

}
