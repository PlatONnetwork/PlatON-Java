package org.platon.p2p.common;

import com.typesafe.config.Config;
import org.platon.common.config.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>LmdbConfig.java</p>
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
 * 2018/5/11, lvxiaoyi, Initial Version.
 */
public class LmdbConfig {
    private static Logger logger = LoggerFactory.getLogger(ReDiRConfig.class);

    private Config config;

    public static LmdbConfig getInstance() {
        return new LmdbConfig(ConfigProperties.getInstance().getConfig().getObject("lmdb").toConfig());
    }

    public LmdbConfig(Config config) {
        this.config = config;
    }


    public String getLmdbNativeLib() {
        return config.getString("lmdbjava-native-lib");
    }

    public String getLmdbName() {
        return config.getString("lmdb-name");
    }

    public String getLmdbDataFile() {
        return config.getString("lmdb-data-file");
    }

    public int getLmdbMaxReaders() {
        return config.getInt("lmdb-max-readers");
    }


}
