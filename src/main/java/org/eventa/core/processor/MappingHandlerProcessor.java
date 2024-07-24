package org.eventa.core.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.registry.CommandHandlerRegistry;
import org.eventa.core.registry.EventHandlerRegistry;
import org.eventa.core.registry.EventSourcingHandlerRegistry;
import org.eventa.core.registry.QueryHandlerRegistry;
import org.eventa.core.streotype.*;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MappingHandlerProcessor implements ApplicationContextAware {
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final EventSourcingHandlerRegistry eventSourcingHandlerRegistry;
    private final EventHandlerRegistry eventHandlerRegistry;
    private final QueryHandlerRegistry queryHandlerRegistry;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        final Map<String, Object> aggregates = ctx.getBeansWithAnnotation(Aggregate.class);
        for (Map.Entry<String, Object> entry : aggregates.entrySet()) {
            final Class<?> aggregateClazz = AopProxyUtils.ultimateTargetClass(entry.getValue());

            final Optional<Field> optionalRoutingKeyField = Arrays.stream(aggregateClazz.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(RoutingKey.class))
                    .findFirst();

            if (optionalRoutingKeyField.isEmpty()) {
                throw new RuntimeException(entry.getValue().getClass().getSimpleName() + " must have routing key for Aggregate Identifier using @RoutingKey annotation.");
            }

            Arrays.stream(aggregateClazz.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(CommandHandler.class))
                    .forEach(method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            commandHandlerRegistry.registerHandler(parameterTypes[0], method);
                        }
                    });

            Arrays.stream(aggregateClazz.getConstructors())
                    .filter(constructor -> constructor.isAnnotationPresent(CommandHandler.class))
                    .forEach(constructor -> {
                        Class<?>[] parameterTypes = constructor.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            commandHandlerRegistry.registerConstructorHandler(parameterTypes[0], constructor);
                        }
                    });

            Arrays.stream(aggregateClazz.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(EventSourcingHandler.class))
                    .forEach(method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            eventSourcingHandlerRegistry.registerHandler(parameterTypes[0], method);
                        }
                    });
        }


        Map<String, Object> projectionGroups = ctx.getBeansWithAnnotation(ProjectionGroup.class);
        for (Map.Entry<String, Object> entry : projectionGroups.entrySet()) {
            final Class<?> projectionClazz = AopProxyUtils.ultimateTargetClass(entry.getValue());
            Arrays.stream(projectionClazz.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(EventHandler.class))
                    .forEach(method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            eventHandlerRegistry.registerHandler(parameterTypes[0], method);
                        }
                    });

            Arrays.stream(projectionClazz.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(QueryHandler.class))
                    .forEach(method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            queryHandlerRegistry.registerHandler(parameterTypes[0], method);
                        }
                    });
        }

    }
}
