package org.platon.core.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.platon.common.config.Validated;
import org.platon.common.config.ConfigProperties;
import org.platon.common.utils.Numeric;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class CoreConfig {

    private Config config;

    public final static String PROPERTY_DB_DIR = "database.dir";
    public final static String PROPERTY_DB_RESET = "database.reset";
    private final static String DEFAULT_CONFIG_KEY = "core";

    private Boolean vmTrace;
    private Boolean recordInternalTransactionsData;

    public static CoreConfig getInstance() {
        return new CoreConfig(ConfigProperties.getInstance().getConfig().getObject(DEFAULT_CONFIG_KEY).toConfig());
    }

    public void overrideParams(Config overrideOptions) {
        config = overrideOptions.withFallback(config);
        validateConfig();
    }

    public void overrideParams(String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) throw new RuntimeException("Odd argument number");
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        overrideParams(map);
    }

    public void overrideParams(Map<String, ?> cliOptions) {
        Config cliConf = ConfigFactory.parseMap(cliOptions);
        overrideParams(cliConf);
    }

    public CoreConfig(Config config) {
        this.config = config;
    }

    public <T> T getProperty(String propName, T defaultValue) {
        if (!config.hasPath(propName)) return defaultValue;
        String string = config.getString(propName);
        if (string.trim().isEmpty()) return defaultValue;
        return (T) config.getAnyRef(propName);
    }

    private void validateConfig() {
        for (Method method : getClass().getMethods()) {
            try {
                if (method.isAnnotationPresent(Validated.class)) {
                    method.invoke(this);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error validating config method: " + method, e);
            }
        }
    }

    public String workDir() {
        return System.getProperty("user.dir");
    }

    @Validated
    public String keystoreDir() {
        return config.getString("keystore.dir");
    }

    @Validated
    public String genesisFileName() {
        return config.getString("genesis.resourcePath");
    }

    @Validated
    public String ancestorBlockCacheSize() {
        return config.getString("validator.timeliness.ancestorBlockCache.size");
    }

    @Validated
    public String pendingBlockCacheSize() {
        return config.getString("validator.timeliness.pendingBlockCache.size");
    }

    @Validated
    public String pendingTxCacheSize() {
        return config.getString("validator.timeliness.pendingTxCache.size");
    }

    @Validated
    public byte[] getMinerCoinbase() {
        String sc = config.getString("mine.coinbase");
        if (StringUtils.isEmpty(sc)) {
            throw new RuntimeException("miner.coinbase has invalid length: '" + sc + "'");
        }
        byte[] c = Numeric.hexStringToByteArray(sc);
        return c;
    }

    @Validated
    public long getMineBlockTime() {
        String stime = config.getString("mine.mineBlockTime");
        if (null == stime || "".equals(stime)) {
            return 1000;
        }
        return Long.valueOf(stime);
    }

    @Validated
    public String databaseDir() {
        return config.getString("database.dir");
    }

    @Validated
    public String databaseSource() {
        return config.getString("database.source");
    }

    public Integer databaseVersion() {
        return config.getInt("database.version");
    }

    public boolean databaseReset() {
        return config.getBoolean("database.reset");
    }

    public long databaseResetBlock() {
        return config.getLong("database.resetBlock");
    }

    @Validated
    public String getKeyValueDataSource() {
        return config.getString("database.source");
    }

    @Validated
    public String getIncompatibleDatabaseBehavior() {
        return config.getString("database.incompatibleDatabaseBehavior");
    }

    @Validated
    public boolean blockChainOnly() {
        String key = "chain.only";
        if (!config.hasPath(key)) return false;
        return config.getBoolean(key);
    }

    public boolean isRecordBlocks() {
        String key = "block.record";
        if (!config.hasPath(key)) return false;
        return config.getBoolean(key);
    }

    public String dumpDir() {
        return config.getString("dump.dir");
    }

    public BigInteger getMineMinEnergonPrice() {
        return new BigInteger(config.getString("mine.minEnergonPrice"));
    }

    @Validated
    public boolean vmTrace() {
        return vmTrace == null ? (vmTrace = config.getBoolean("vm.structured.trace")) : vmTrace;
    }

    @Validated
    public boolean recordInternalTransactionsData() {
        if (recordInternalTransactionsData == null) {
            recordInternalTransactionsData = config.getBoolean("record.internal.transactions.data");
        }
        return recordInternalTransactionsData;
    }

    @Validated
    public int dumpBlock() {
        return config.getInt("dump.block");
    }

    @Validated
    public String dumpStyle() {
        return config.getString("dump.style");
    }

    public void setRecordInternalTransactionsData(Boolean recordInternalTransactionsData) {
        this.recordInternalTransactionsData = recordInternalTransactionsData;
    }

    public Config getConfig() {
        return this.config;
    }

}
