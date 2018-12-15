package org.platon.p2p.plugins;

/**
 * @author yangzhou
 * @create 2018-04-24 15:50
 */
public abstract class PluginFactory {
    PluginFactory factory;

    void register(PluginFactory factory) {
        this.factory = factory;
    }


    TopologyPlugin create() {
        return factory.create();
    }
}
