package org.platon.core.facade;

import org.platon.common.AppenderName;
import org.platon.common.config.ConfigProperties;
import org.platon.core.config.DefaultConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class PlatonFactory {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    public static Platon createPlaton() {
        return createPlaton((Class) null);
    }

    public static Platon createPlaton(Class userSpringConfig) {
        return userSpringConfig == null ?
                createPlaton(new Class[] {DefaultConfig.class}) :
                createPlaton(DefaultConfig.class, userSpringConfig);
    }

    public static Platon createPlaton(ConfigProperties config, Class userSpringConfig) {
        return userSpringConfig == null ?
                createPlaton(new Class[] {DefaultConfig.class}) :
                createPlaton(DefaultConfig.class, userSpringConfig);
    }

    public static Platon createPlaton(Class ... springConfigs) {
        logger.info("~> Starting Platonj...");
        ApplicationContext context = new AnnotationConfigApplicationContext(springConfigs);
        return context.getBean(Platon.class);
    }
}
