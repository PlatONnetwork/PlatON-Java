package org.platon.core.config;

import org.platon.core.Repository;
import org.platon.core.block.GenesisBlock;
import org.platon.core.genesis.GenesisJson;
import org.platon.core.genesis.GenesisLoader;

/**
 * SystemConfig
 *
 * @author yanze
 * @desc loading system config
 * @create 2018-07-31 16:58
 */
public class SystemConfig {

    private BlockchainNetConfig blockchainConfig;
    private GenesisJson genesisJson;
    private GenesisBlock genesisBlock;
    private static SystemConfig systemConfig;
    private static boolean useOnlySpringConfig = false;

    private CoreConfig config;

    /**
     * Returns the static config instance. If the config is passed
     * as a Spring bean by the application this instance shouldn't
     * be used
     * This method is mainly used for testing purposes
     * (Autowired fields are initialized with this static instance
     * but when running within Spring context they replaced with the
     * bean config instance)
     */
    public static SystemConfig getDefault() {
        return useOnlySpringConfig ? null : getSpringDefault();
    }

    public static SystemConfig getSpringDefault() {
        if (systemConfig == null) {
            systemConfig = new SystemConfig();
        }
        return systemConfig;
    }

    public BlockchainNetConfig getBlockchainConfig(){
        if(blockchainConfig == null){
            //loading blockchain config
            GenesisJson genesisJson = getGenesisJson();
            if(genesisJson.getJsonBlockchainConfig() != null){
                //TODO blockchainConfig or BlockchainNetConfig
//                blockchainConfig = genesisJson.getJsonBlockchainConfig();
            }else{
                //TODO default config and add another config
                throw new RuntimeException("system config is null");
            }
        }
        return blockchainConfig;
    }

    public GenesisJson getGenesisJson() {
        if(genesisJson == null){
            genesisJson = GenesisLoader.loadGenesisJson(CoreConfig.getInstance().genesisFileName());
        }
        return genesisJson;
    }

    public GenesisBlock getGenesisBlock(Repository repository) {
        if (genesisBlock == null) {
            genesisBlock = GenesisLoader.loadGenesisBlock(repository, getGenesisJson());
        }
        return genesisBlock;
    }

    static boolean isUseOnlySpringConfig() {
        return useOnlySpringConfig;
    }

    public static void setUseOnlySpringConfig(boolean useOnlySpringConfig) {
        SystemConfig.useOnlySpringConfig = useOnlySpringConfig;
    }

}
