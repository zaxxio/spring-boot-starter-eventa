package org.eventa.core.registry;

import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QueryHandlerRegistry {
    private final ConcurrentHashMap<Class<?>, List<Method>> routes = new ConcurrentHashMap<>();

    public void registerHandler(Class<?> type, Method method) {
        routes.computeIfAbsent(type, methods -> new LinkedList<>()).add(method);
    }

    public Method getHandler(Class<?> commandType) {
        List<Method> methods = routes.get(commandType);
        if (methods == null) {
            throw new RuntimeException(String.format("%s Query Handler is not found.", commandType.getName()));
        }
        if (methods.size() > 1) {
            throw new RuntimeException("More than one Command handler is registered");
        }
        return methods.get(0);
    }
}
