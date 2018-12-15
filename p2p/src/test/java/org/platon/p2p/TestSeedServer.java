package org.platon.p2p;

import org.platon.common.config.ConfigProperties;

public class TestSeedServer {
    public static void main(String[] args){
        ConfigProperties.setConfigPath("");
        NodeServer server1 = new NodeServer();
        server1.startup();
    }
}
