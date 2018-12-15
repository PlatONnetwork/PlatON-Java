package org.platon.core.config;

import org.platon.common.AppenderName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

class Initializer implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    private void initConfig(SystemConfig config) {

        logger.info("~> initConfig(SystemConfig config)...");

        //loading blockchain config
        config.getBlockchainConfig();

        //todo: 测试屏蔽
        //loading genesis config
        //config.getGenesisBlock();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SystemConfig) {
            initConfig((SystemConfig) bean);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
