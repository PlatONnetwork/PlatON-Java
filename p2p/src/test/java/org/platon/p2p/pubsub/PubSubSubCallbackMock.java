package org.platon.p2p.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangzhou
 * @create 2018-08-02 17:15
 */

public class PubSubSubCallbackMock implements PubSub.SubscribeCallback {
    private static Logger logger = LoggerFactory.getLogger(PubSubSubCallbackMock.class);

    @Override
    public void subscribe(String topic, byte[] data) {
        logger.trace("receive new data:{}", topic);
//        ServiceDiscoveryManager serviceDiscoveryManager = BeanLocator.getBean("serviceDiscoveryManager");
//        serviceDiscoveryManager.subscribe(topic, data);
    }
}