package org.eventa.core.dispatcher.impl;

import lombok.RequiredArgsConstructor;

import org.eventa.core.dispatcher.QueryDispatcher;
import org.eventa.core.dispatcher.ResponseType;
import org.eventa.core.registry.QueryHandlerRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@RequiredArgsConstructor
public class QueryDispatcherImpl implements QueryDispatcher {
    private final QueryHandlerRegistry queryHandlerRegistry;
    private final ApplicationContext applicationContext;
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private Lock readLock = readWriteLock.readLock();

    @Override
    public <Q, R> R dispatch(Q query, ResponseType<R> responseType) {
        Method queryMethod = queryHandlerRegistry.getHandler(query.getClass());
        if (queryMethod == null) {
            throw new RuntimeException("No handler found for query: " + query.getClass().getName());
        }
        try {
            this.readLock.lock();
            Object handlerBean = this.applicationContext.getBean(queryMethod.getDeclaringClass());
            Object result = queryMethod.invoke(handlerBean, query);
            return responseType.convert(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke handler for query: " + query.getClass().getName(), e);
        } finally {
            this.readLock.unlock();
        }
    }
}
