package org.eventa.core.dispatcher.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.dispatcher.EventDispatcher;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.registry.EventHandlerRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

@Log4j2
@Component
@RequiredArgsConstructor
public class EventDispatcherImpl implements EventDispatcher {
    private final EventHandlerRegistry eventHandlerRegistry;
    private final ApplicationContext applicationContext;

    @Override
    public void dispatch(BaseEvent baseEvent) {
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    handleEvent(baseEvent);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).join();
        } catch (Exception e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private void handleEvent(BaseEvent baseEvent) throws Exception {
        Method handler = eventHandlerRegistry.getHandler(baseEvent.getClass());
        Object bean = this.applicationContext.getBean(handler.getDeclaringClass());
        handler.invoke(bean, baseEvent);
    }
}
