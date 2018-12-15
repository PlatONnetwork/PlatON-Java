package org.platon.core.facade;

import org.junit.Assert;
import org.junit.Test;
import org.platon.core.config.CommonConfig;
import org.platon.core.config.DefaultConfig;

public class PlatonFactoryTest {

    @Test
    public void testCreateByNoConfig(){
        Platon platon = PlatonFactory.createPlaton();
        Assert.assertNotNull(platon);
    }

    @Test
    public void testCreateByDefaultConfig(){
        Platon platon = PlatonFactory.createPlaton(DefaultConfig.class);
        Assert.assertNotNull(platon);
    }

    @Test
    public void testCreateByCommonConfig(){
        Platon platon = PlatonFactory.createPlaton(CommonConfig.class);
        Assert.assertNotNull(platon);
    }


}