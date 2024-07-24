package org.eventa.core.streotype;

import org.eventa.core.config.EventaAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import(EventaAutoConfiguration.class)
public @interface EnableEventSourcing {

}
