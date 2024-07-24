package org.eventa.core.interceptor;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.CopyOnWriteArrayList;

@Log4j2
@Data
public class CommandInterceptorRegisterer {
    private CopyOnWriteArrayList<CommandInterceptor> commandInterceptors = new CopyOnWriteArrayList<>();

    public void register(CommandInterceptor commandInterceptor) {
        this.commandInterceptors.add(commandInterceptor);
    }
}
