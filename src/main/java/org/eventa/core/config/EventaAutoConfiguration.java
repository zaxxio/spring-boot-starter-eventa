package org.eventa.core.config;

import lombok.extern.log4j.Log4j2;
import org.eventa.core.config.props.EventaProperties;
import org.eventa.core.eventstore.EventStore;
import org.eventa.core.interceptor.CommandInterceptor;
import org.eventa.core.interceptor.CommandInterceptorRegisterer;
import org.eventa.core.producer.EventProducer;
import org.eventa.core.producer.KafkaEventProducer;
import org.eventa.core.streotype.Interceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Map;

@Log4j2
@AutoConfiguration
@ConfigurationPropertiesScan
@EnableMongoRepositories(basePackageClasses = {EventModelRepository.class})
@ComponentScan(basePackages = "org.eventa.core")
@EnableConfigurationProperties(EventaProperties.class)
public class EventaAutoConfiguration implements ApplicationContextAware, BeanFactoryAware, DisposableBean {

    private BeanFactory beanFactory;
    private ApplicationContext applicationContext;

    @Bean
    @ConditionalOnMissingBean
    public CommandInterceptorRegisterer commandInterceptorRegisterer() {
        final Map<String, Object> commandInterceptors = this.applicationContext.getBeansWithAnnotation(Interceptor.class);
        CommandInterceptorRegisterer commandInterceptorRegisterer = new CommandInterceptorRegisterer();
        for (Map.Entry<String, Object> entry : commandInterceptors.entrySet()) {
            Object value = entry.getValue();
            commandInterceptorRegisterer.register((CommandInterceptor) value);
        }
        log.info("Found {} command interceptor.", commandInterceptors.size());
        return commandInterceptorRegisterer;
    }

    @Bean
    @ConditionalOnMissingBean
    public EventStore mongoEventStore() {
        return new MongoEventStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventProducer eventStore() {
        return new KafkaEventProducer();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void destroy() throws Exception {

    }
}
