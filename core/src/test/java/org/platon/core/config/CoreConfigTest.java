package org.platon.core.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@Ignore
public class CoreConfigTest {

    @Test
    public void test01(){
        System.out.println("work.dir:" + System.getProperty("user.dir"));
        CoreConfig coreConfig = CoreConfig.getInstance();
        System.out.println("database.dir:" + coreConfig.databaseDir());

        // tips: for override.
        Map<String, String> override = new HashMap<>();
        override.put("database.dir", "otherdir");
        coreConfig.overrideParams(override);
        Assert.assertEquals("otherdir",coreConfig.databaseDir());

        // tips:
        Config cliConf = ConfigFactory.parseString("a={a1=1,a2=2}");
        coreConfig.overrideParams(cliConf);
        String a1 = coreConfig.getConfig().getString("a.a1");
        System.out.println(a1);
        Assert.assertEquals("1",a1);
    }
}