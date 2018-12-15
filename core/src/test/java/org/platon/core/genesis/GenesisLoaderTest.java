package org.platon.core.genesis;

import org.junit.Assert;
import org.junit.Test;

public class GenesisLoaderTest {

    private final String genesisName = "genesis.json";

    @Test
    public void loadGenesisJson() {
        GenesisJson genesisJson = GenesisLoader.loadGenesisJson(genesisName);
        Assert.assertNotNull(genesisJson.getAccounts());
    }

}