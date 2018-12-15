package org.platon.core.config;

import org.platon.common.AppenderName;
import org.platon.common.config.IgnoreLoad;
import org.platon.common.wrapper.DataWord;
import org.platon.core.Repository;
import org.platon.core.block.BlockPool;
import org.platon.core.datasource.SourceCodec;
import org.platon.core.db.*;
import org.platon.core.vm.program.ProgramPrecompile;
import org.platon.crypto.HashUtil;
import org.platon.storage.datasource.*;
import org.platon.storage.datasource.inmemory.HashMapDB;
import org.platon.storage.datasource.leveldb.LevelDBSource;
import org.platon.storage.datasource.rocksdb.RocksDBSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.HashSet;
import java.util.Set;


@Configuration
@EnableTransactionManagement
@ComponentScan(
		basePackages = "org.platon",
		excludeFilters = @ComponentScan.Filter(IgnoreLoad.class))
public class CommonConfig {

	private static final Logger logger = LoggerFactory.getLogger(AppenderName.APPENDER_PLATIN);

    private Set<DbSource> dbSources = new HashSet<>();

    private static CommonConfig INSTANCE;

    public static CommonConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CommonConfig();
        }
        return INSTANCE;
    }

	@Bean
	public SystemConfig systemConfig() {
		return SystemConfig.getSpringDefault();
	}

	@Bean
	BeanPostProcessor initializer() {
		return new Initializer();
	}

	@Bean
	public BlockPool blockPool() {

		return new BlockPool(1024);
	}



    @Bean @Primary
    public Repository repository() {
        return new RepositoryWrapper();
    }

    @Bean
    public Repository defaultRepository() {
        return new RepositoryRoot(stateSource(), null);
    }

    @Bean @Scope("prototype")
    public Repository repository(byte[] stateRoot) {
        return new RepositoryRoot(stateSource(), stateRoot);
    }

    @Bean
    @Scope("prototype")
    public Source<byte[], byte[]> cachedDbSource(String name) {

        AbstractCachedSource<byte[], byte[]> writeCache = new AsyncWriteCache<byte[], byte[]>(blockchainSource(name)) {
            @Override
            protected WriteCache<byte[], byte[]> createCache(Source<byte[], byte[]> source) {
                WriteCache.BytesKey<byte[]> ret = new WriteCache.BytesKey<>(source, WriteCache.CacheType.SIMPLE);
                ret.withSizeEstimators(MemSizeEstimator.ByteArrayEstimator, MemSizeEstimator.ByteArrayEstimator);
                ret.setFlushSource(true);
                return ret;
            }
        }.withName(name);
        dbFlushManager().addCache(writeCache);
        return writeCache;
    }

    @Bean
    public StateSource stateSource() {
        StateSource stateSource = new StateSource(blockchainSource("state"), false);
        dbFlushManager().addCache(stateSource.getWriteCache());
        return stateSource;
    }

    @Bean
    @Scope("prototype")
    public Source<byte[], byte[]> blockchainSource(String name) {
        return new XorDataSource<>(blockchainDbCache(), HashUtil.sha3(name.getBytes()));
    }

    @Bean
    @Scope("prototype")
    protected LevelDBSource levelDbDataSource(String name) {
        return new LevelDBSource(name,CoreConfig.getInstance().workDir());
    }

    @Bean
    @Scope("prototype")
    protected RocksDBSource rocksDbDataSource(String name) {
        return new RocksDBSource(name, CoreConfig.getInstance().workDir());
    }

    @Bean
    @Scope("prototype")
    @Primary
    public DbSource<byte[]> keyValueStorage(String name, DbSettings settings) {
        String dataSource = CoreConfig.getInstance().getKeyValueDataSource();
        try {
			DbSource<byte[]> dbSource;
            if ("inmem".equals(dataSource)) {
				dbSource = new HashMapDB<>();
            } else if ("leveldb".equals(dataSource)){
				dbSource = levelDbDataSource(name);
            } else {
                dataSource = "rocksdb";
				dbSource = rocksDbDataSource(name);
            }
			dbSource.open(settings);
            dbSources.add(dbSource);
            return dbSource;
        } finally {
            logger.info(dataSource + " key-value data source created: " + name);
        }
    }

    @Bean
    public DbSource<byte[]> blockchainDB() {

        DbSettings settings = DbSettings.newInstance()
                .withMaxOpenFiles(512)
                .withMaxThreads(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        return keyValueStorage("blockchain", settings);
    }

    @Bean
    public AbstractCachedSource<byte[], byte[]> blockchainDbCache() {

        WriteCache.BytesKey<byte[]> ret = new WriteCache.BytesKey<>(
                new BatchSourceWriter<>(blockchainDB()), WriteCache.CacheType.SIMPLE);
        ret.setFlushSource(true);

        return ret;
    }

    @Bean
    public CoreConfig config(){
	    return CoreConfig.getInstance();
    }

    @Bean
    public DbFlushManager dbFlushManager() {
        return new DbFlushManager(config(), dbSources, blockchainDbCache());
    }

	@Bean
	@Lazy
	public DbSource<byte[]> headerStorage() {
		return keyValueStorage("headers",DbSettings.DEFAULT);
	}

	@Bean
	@Lazy
	public HeaderStore headerStore() {

		DbSource<byte[]> dataStorage = headerStorage();

		WriteCache.BytesKey<byte[]> cache = new WriteCache.BytesKey<>(
				new BatchSourceWriter<>(dataStorage), WriteCache.CacheType.SIMPLE);
		cache.setFlushSource(true);


		dbFlushManager().addCache(cache);

		HeaderStore headerStore = new HeaderStore();
		Source<byte[], byte[]> headers = new XorDataSource<>(cache, HashUtil.sha3("header".getBytes()));
		Source<byte[], byte[]> index = new XorDataSource<>(cache, HashUtil.sha3("index".getBytes()));
		headerStore.init(index, headers);

		return headerStore;
	}

    @Bean
    public Source<byte[], ProgramPrecompile> precompileSource() {

        StateSource source = stateSource();
        return new SourceCodec<byte[], ProgramPrecompile, byte[], byte[]>(source,
                new SerializerIfc<byte[], byte[]>() {
                    public byte[] serialize(byte[] object) {
                        DataWord ret = DataWord.of(object);
                        ret.add(DataWord.of(1));
                        return ret.getLast20Bytes();
                    }
                    public byte[] deserialize(byte[] stream) {
                        throw new RuntimeException("Shouldn't be called");
                    }
                }, new SerializerIfc<ProgramPrecompile, byte[]>() {
            public byte[] serialize(ProgramPrecompile object) {
                return object == null ? null : object.serialize();
            }
            public ProgramPrecompile deserialize(byte[] stream) {
                return stream == null ? null : ProgramPrecompile.deserialize(stream);
            }
        });
    }

}
