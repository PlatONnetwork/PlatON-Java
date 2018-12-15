package org.platon.p2p.plugins;

/**
 * @author yangzhou
 * @create 2018-04-24 15:51
 */
public class TopologyPluginFactory extends PluginFactory {

    @Override
    public TopologyPlugin create() {
        return new KadTopologyPlugin();
    }
}
