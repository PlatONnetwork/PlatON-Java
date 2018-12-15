package org.platon.common.config;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeConfig {
    private static Logger logger = LoggerFactory.getLogger(NodeConfig.class);

    private Config config;

    public static NodeConfig getInstance() {
        return new NodeConfig(ConfigProperties.getInstance().getConfig().getObject("node").toConfig());
    }

    private NodeConfig(Config config) {
        this.config = config;
    }

    public String getHost() {
        return config.getString("host");
    }

    public String getPublicKey() {
        return config.getString("public-key");
    }

    public String getPrivateKey() {
        return config.getString("private-key");
    }
}
