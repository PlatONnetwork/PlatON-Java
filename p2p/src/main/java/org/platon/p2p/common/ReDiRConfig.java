package org.platon.p2p.common;

import com.typesafe.config.Config;
import org.platon.common.config.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

/**
 * <p>ReDiRConfig.java</p>
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
public class ReDiRConfig {

    private static Logger logger = LoggerFactory.getLogger(ReDiRConfig.class);

    private Config config;

    public static ReDiRConfig getInstance() {
        return new ReDiRConfig(ConfigProperties.getInstance().getConfig().getObject("redir").toConfig());
    }

    public ReDiRConfig(Config config) {
        this.config = config;
    }


    public int getBranchingFactor(String namespace) {
        return config.getInt(namespace + ".branching-factor");
    }

    public int getLevel(String namespace) {
        return config.getInt(namespace + ".level");
    }


    public BigInteger getLowestKey(String namespace) {
        return new BigInteger(config.getString(namespace + ".lowest-key"));
    }


    public BigInteger getHighestKey(String namespace) {
        return new BigInteger(config.getString(namespace + ".highest-key"));
    }


    public int getStartLevel(String namespace) {
        return config.getInt(namespace + ".start-level");
    }

    public String getAlgorithm(String namespace) {
        return config.getString(namespace + ".algorithm");
    }

}