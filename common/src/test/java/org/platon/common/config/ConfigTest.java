package org.platon.common.config;

import com.typesafe.config.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ConfigTest {

    @Test
    public void test01() {


        Config config = ConfigFactory.parseResources("test-platon.conf");
        System.out.println(config.root().render(ConfigRenderOptions.defaults().setComments(true)));

        for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
            System.out.println("~> Name:  " + entry.getKey());
            System.out.println("~> Value: " + entry.getValue());
        }


        List<? extends ConfigObject> list = config.getObjectList("peer.active.list");
        for (ConfigObject configObject : list) {
            if (configObject.get("ip") != null) {
                System.out.println("ip: " + configObject.toConfig().getString("ip"));
            }
            if (configObject.get("port") != null) {
                int port = configObject.toConfig().getInt("port");
                System.out.println("port: " + port);
            }
            if (configObject.get("public-key") != null) {
                String publicKey = configObject.toConfig().getString("public-key");
                System.out.println("public-key: " + publicKey);
            }
        }
    }

    @Test
    public void test02() {


        System.setProperty("onekey", "Hello Key.");

        Config config = ConfigFactory.load("test-platon.conf");
        String string = config.getString("core.keystore.dir");
        Assert.assertNotNull(string);


        Config overrides = ConfigFactory.parseString("onekey='Hello updated key.', peer.active=[{url=sdfsfd}]");
        Config merged = overrides.withFallback(config);

        System.out.println("onekey : " + merged.getString("onekey"));
        Assert.assertNotEquals("Hello Key.", merged.getString("onekey"));
    }

}
