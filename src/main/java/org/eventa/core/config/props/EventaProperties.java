package org.eventa.core.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "eventa")
public class EventaProperties {
    private String commandBus;
    private String eventBus;
}
