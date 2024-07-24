package org.eventa.core.handler;

import lombok.RequiredArgsConstructor;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.eventstore.EventStore;
import org.eventa.core.producer.KafkaEventProducer;
import org.eventa.core.streotype.ProjectionGroup;
import org.eventa.core.streotype.ResetHandler;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class EventProcessingHandler {

    private final EventStore eventStore;
    private final KafkaEventProducer kafkaEventProducer;
    private final ApplicationContext ctx;
    private final Set<Method> resetHandlers = new HashSet<>();

    public EventProcessingHandler eventProcessor(String processorName) {
        final Map<String, Object> projectionGroups = ctx.getBeansWithAnnotation(ProjectionGroup.class);
        for (Map.Entry<String, Object> entry : projectionGroups.entrySet()) {
            final Class<?> projectionClazz = AopProxyUtils.ultimateTargetClass(entry.getValue());
            ProjectionGroup projectionGroup = projectionClazz.getAnnotation(ProjectionGroup.class);
            if (Objects.equals(projectionGroup.name(), processorName)) {
                Stream<Method> methodStream = Arrays.stream(projectionClazz.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(ResetHandler.class));
                resetHandlers.addAll(methodStream.toList());
            }
        }
        return this;
    }

    public void reset() {
        CompletableFuture.runAsync(() -> {
            for (Method resetHandler : resetHandlers) {
                Object bean = ctx.getBean(resetHandler.getDeclaringClass());
                try {
                    resetHandler.invoke(bean);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
            final List<BaseEvent> eventStream = eventStore.getEvents();
            for (BaseEvent baseEvent : eventStream) {
                kafkaEventProducer.produceEvent("BaseEvent", baseEvent);
            }
        });
    }

}
