package org.eventa.core.streotype;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Service
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public @interface ProjectionGroup {
    String name();
}
