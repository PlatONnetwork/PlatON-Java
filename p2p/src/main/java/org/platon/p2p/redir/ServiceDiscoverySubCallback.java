package org.platon.p2p.redir;

import org.platon.p2p.pubsub.PubSub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangzhou
 * @create 2018-07-25 19:24
 */
public class ServiceDiscoverySubCallback implements PubSub.SubscribeCallback {
    private static Logger logger = LoggerFactory.getLogger(ServiceDiscoverySubCallback.class);

    @Override
    public void subscribe(String topic, byte[] data) {

        logger.trace("receive new data:{}", topic);


    }
}
