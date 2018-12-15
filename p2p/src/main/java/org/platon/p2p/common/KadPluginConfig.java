package org.platon.p2p.common;

import com.typesafe.config.Config;
import org.platon.common.config.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>KadPluginConfig.java</p>
 * <p/>
 * <p>Description:</p>
 * <p/>
 * <p>Copyright (c) 2017. juzix.io. All rights reserved.</p>
 * <p/>
 *
 * @version 1.0.0
 * @author: lvxiaoyi
 * <p/>
 * Revision History:
 * 2018/5/2, lvxiaoyi, Initial Version.
 */
public class KadPluginConfig {
    private static Logger logger = LoggerFactory.getLogger(KadPluginConfig.class);

    private Config config;


    public static KadPluginConfig getInstance() {
        return new KadPluginConfig(ConfigProperties.getInstance().getConfig().getObject("kad.plugin").toConfig());
    }

    KadPluginConfig(Config config) {
        this.config = config;
    }




    public int getIdLength() {
       return config.getInt("id-length");
    }

    /**
     * @return Interval in milliseconds between execution of RestoreOperations.
     */
    public long getRestoreInterval() {
        return config.getLong("restore-interval");
    }


    /**
     * If no reply received from a node in this period (in milliseconds)
     * consider the node unresponsive.
     *
     * @return The time it takes to consider a node unresponsive
     */
    public long getResponseTimeout() {
        return config.getLong("response-timeout");
    }

    /**
     * @return Maximum number of milliseconds for performing an operation.
     */
    public long getOperationTimeout() {
        return config.getLong("operation-timeout");
    }

    /**
     * @return Maximum number of concurrent messages in transit.
     */
    public int maxConcurrentMessagesTransiting() {
        return config.getInt("max-concurrent-messages-transiting");
    }

    /**
     * @return K-Value used throughout Kademlia
     */
    public int getKValue() {
        return config.getInt("K-value");
    }

    /**
     * @return Size of replacement cache.
     */
    public int getReplacementCacheSize() {
        return config.getInt("replacement-cache-size");
    }

    /**
     * @return # of times a node can be marked as stale before it is actually removed.
     */
    public int getStaleTimes() {
        return config.getInt("stale-times");
    }
}
