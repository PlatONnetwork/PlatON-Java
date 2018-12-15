package org.platon.core.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Assert;
import org.junit.Test;
import org.platon.common.AppenderName;
import org.platon.core.rpc.ProtoRpcImpl;
import org.platon.core.rpc.ProtoRpc;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;

public class DefaultConfigTest {

    public static class TestConfig{

        @Bean
        public ProtoRpc protoRpc(){
            return new ProtoRpcImpl();
        }

    }

    @Test
    public void testConstruction() throws InterruptedException {



        ListAppender<ILoggingEvent> memoryAppender = new ListAppender<>();
        memoryAppender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);
        try {
            logger.setLevel(Level.DEBUG);
            logger.addAppender(memoryAppender);

            new DefaultConfig();

            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    throw new IllegalStateException("unit test throw exception");
                }
            });
            Thread.sleep(500);
            ILoggingEvent first = memoryAppender.list.get(0);
            Assert.assertEquals("Uncaught exception", first.getMessage());

            IThrowableProxy cause = first.getThrowableProxy();
            System.out.println(cause.getMessage());
            Assert.assertEquals("unit test throw exception",cause.getMessage());
        } finally {
            memoryAppender.stop();
            logger.detachAppender(memoryAppender);
        }
    }

    @Test
    public void testDefaultInject(){
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class,TestConfig.class);
        DefaultConfig config = context.getBean(DefaultConfig.class);
        TestConfig testConfig = context.getBean(TestConfig.class);
        ProtoRpc protoRpc = context.getBean(ProtoRpcImpl.class);
        ProtoRpc protoRpc1 = testConfig.protoRpc();
        Assert.assertNotEquals(protoRpc,protoRpc1);
        Assert.assertNotNull(config);
        Assert.assertNotNull(config.getAppCtx());
    }
}