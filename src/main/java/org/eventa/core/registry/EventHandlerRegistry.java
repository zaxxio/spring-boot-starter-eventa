package org.eventa.core.registry;

import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventHandlerRegistry {
    private final ConcurrentHashMap<Class<?>, List<Method>> routes = new ConcurrentHashMap<>();

    public void registerHandler(Class<?> type, Method method) {
        routes.computeIfAbsent(type, methods -> new LinkedList<>()).add(method);
    }

    public boolean isRegistered(Class<?> commandType) {
        return routes.containsKey(commandType);
    }

    public Method getHandler(Class<?> commandType) {
        try {
            List<Method> methods = routes.get(commandType);
            if (methods == null) {
                throw new RuntimeException(String.format("%s Event Handler is not found.", commandType.getName()));
            }
            if (methods.size() > 1) {
                throw new RuntimeException("More than one Event handler is registered");
            }
            return methods.get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
