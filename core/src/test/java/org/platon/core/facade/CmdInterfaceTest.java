package org.platon.core.facade;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.platon.core.config.CoreConfig;

@Ignore
public class CmdInterfaceTest {

    @Test
    public void testCliParseForHelp(){

        String[] cmds = new String[]{"--help"};
        CmdInterface.call(cmds);
    }

    @Test
    public void testCliParse(){

        System.setProperty(CoreConfig.PROPERTY_DB_DIR, "db-00001");
        System.setProperty(CoreConfig.PROPERTY_DB_RESET, "yes");

        String[] cmds = new String[]{"-db","db-00002","-reset","no"};
        CmdInterface.call(cmds);

        Assert.assertEquals("db-00002", CoreConfig.getInstance().getConfig().getString(CoreConfig.PROPERTY_DB_DIR));
        Assert.assertEquals(false, CoreConfig.getInstance().getConfig().getBoolean(CoreConfig.PROPERTY_DB_RESET));
    }
}