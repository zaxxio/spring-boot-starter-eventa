package org.eventa.core.interceptor;

import org.eventa.core.commands.BaseCommand;

public interface CommandInterceptor {
    void commandIntercept(BaseCommand baseCommand) throws Exception;
}
