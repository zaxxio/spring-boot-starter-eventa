package org.eventa.core.dispatcher.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.aggregate.AggregateRoot;
import org.eventa.core.dispatcher.CommandDispatcher;
import org.eventa.core.commands.BaseCommand;
import org.eventa.core.commands.CommandMessage;
import org.eventa.core.commands.CommandResultMessage;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.eventstore.EventStore;
import org.eventa.core.interceptor.CommandInterceptorRegisterer;
import org.eventa.core.registry.CommandHandlerRegistry;
import org.eventa.core.registry.CommandHandlerType;
import org.eventa.core.streotype.RoutingKey;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

@Log4j2
@Component
@RequiredArgsConstructor
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class CommandDispatcherImpl implements CommandDispatcher {

    private final CommandInterceptorRegisterer commandInterceptorRegisterer;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ConfigurableApplicationContext context;
    private final EventStore eventStore;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private final Lock writeLock = readWriteLock.writeLock();
    private final Lock readLock = readWriteLock.readLock();


    @Override
    public <T extends BaseCommand> void dispatch(T baseCommand, BiConsumer<CommandMessage<T>, CommandResultMessage<?>> callback) throws Exception {
        try {
            this.writeLock.lock();
            final UUID routingKey = extractRoutingKey(baseCommand);
            baseCommand.setMessageId(routingKey);
            final CommandHandlerType commandHandler = this.commandHandlerRegistry.getHandler(baseCommand.getClass());
            preHandle(baseCommand);
            if (commandHandler.handlerType() == CommandHandlerType.HandlerType.METHOD) {
                final Method registryMethodHandler = commandHandler.getCommandHandlerMethod();
                final AggregateRoot aggregateRoot = context.getBean(registryMethodHandler.getDeclaringClass().asSubclass(AggregateRoot.class));
                final List<BaseEvent> historicalEvents = eventStore.getEvents(baseCommand.getMessageId());
                aggregateRoot.replayEvents(historicalEvents);
                registryMethodHandler.invoke(aggregateRoot, baseCommand);
                final String key = eventStore.saveEvents(aggregateRoot.getAggregateIdentifier(), aggregateRoot.getUncommittedChanges(), aggregateRoot.getVersion() - 1, registryMethodHandler.getDeclaringClass().getName(), false);
                aggregateRoot.markChangesAsCommitted();
                callback.accept(new CommandMessage<>(baseCommand), new CommandResultMessage<>(key));
            } else {
                final Constructor<?> registryCommandHandler = commandHandler.getCommandHandlerConstructor();
                final AggregateRoot aggregateRoot = context.getBean(registryCommandHandler.getDeclaringClass().asSubclass(AggregateRoot.class), baseCommand);
                final String key = eventStore.saveEvents(aggregateRoot.getAggregateIdentifier(), aggregateRoot.getUncommittedChanges(), aggregateRoot.getVersion() - 1, registryCommandHandler.getDeclaringClass().getName(), true);
                aggregateRoot.markChangesAsCommitted();
                callback.accept(new CommandMessage<>(baseCommand), new CommandResultMessage<>(key));
            }
        } catch (Exception ex) {
            log.error(ex.getCause());
            callback.accept(new CommandMessage<>(baseCommand), new CommandResultMessage<>(ex.getCause()));
        } finally {
            this.writeLock.unlock();
        }
    }

    private synchronized <T extends BaseCommand> UUID extractRoutingKey(T command) throws IllegalAccessException {
        Class<? extends BaseCommand> aClass = command.getClass();
        for (Field field : aClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(RoutingKey.class)) {
                field.setAccessible(true);
                return (UUID) field.get(command);
            }
        }
        throw new IllegalAccessException("Command must have @RoutingKey for aggregate mapping.");
    }

    private synchronized <T extends BaseCommand> void preHandle(T baseCommand) {
        this.commandInterceptorRegisterer.getCommandInterceptors().forEach(commandInterceptor -> {
            try {
                commandInterceptor.commandIntercept(baseCommand);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
