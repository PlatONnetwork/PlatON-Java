package org.platon.core.config;

import org.platon.common.AppenderName;
import org.platon.core.datasource.TransactionStore;
import org.platon.core.db.BlockStoreIfc;
import org.platon.core.db.BlockStoreImpl;
import org.platon.storage.datasource.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CommonConfig.class)
public class DefaultConfig {

    private static Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    @Autowired
    CommonConfig commonConfig;

    @Autowired
    ApplicationContext appCtx;

    public DefaultConfig() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception", e));
    }

    public ApplicationContext getAppCtx(){
        return appCtx;
    }

    @Bean
    public BlockStoreIfc blockStore(){
        BlockStoreImpl indexedBlockStore = new BlockStoreImpl();
        Source<byte[], byte[]> block = commonConfig.cachedDbSource("block");
        Source<byte[], byte[]> index = commonConfig.cachedDbSource("index");
        indexedBlockStore.init(index, block);
        return indexedBlockStore;
    }

    @Bean
    public TransactionStore transactionStore() {
        return new TransactionStore(commonConfig.cachedDbSource("transactions"));
    }
}
