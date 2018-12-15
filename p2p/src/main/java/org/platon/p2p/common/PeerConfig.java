package org.platon.p2p.common;

import com.typesafe.config.Config;
import org.platon.common.config.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PeerConfig {
    private static Logger logger = LoggerFactory.getLogger(PeerConfig.class);

    private Config config;

    public static PeerConfig getInstance() {
        return new PeerConfig(ConfigProperties.getInstance().getConfig().getObject("peer").toConfig());
    }

    public PeerConfig(Config config) {
        this.config = config;
    }


    public int getPort() {
        return config.getInt("listen.port");
    }

    public List<Config> getActiveNodeConfig() {
        return (List<Config>) config.getConfigList("active.list");
    }

    public int getCreateSessionTimeout() {
        return config.getInt("create.session.timeout");
    }
    public int getMessageResponseTimeout() {
        return config.getInt("message.response.timeout");
    }
    public int getPeerConnectTimeout() {
        return config.getInt("peer.connect.timeout");
    }

    public int getTimeIntervalForDuplicatedMessage() {
        return config.getInt("time.interval.for.duplicated.message");
    }

}
