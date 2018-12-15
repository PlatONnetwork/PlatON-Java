package org.platon.p2p.pubsub;

import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.handler.PlatonMessageType;
import org.platon.p2p.proto.pubsub.TopicMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yangzhou
 * @create 2018-07-24 17:56
 */
@Component("pubSubService")
public class PubSubService {



    @Autowired
    private PubSub pubSub;

    public void setPubSub(PubSub pubSub) {
        this.pubSub = pubSub;
    }

    @PlatonMessageType("TopicMessage")
    public void sendMessage(TopicMessage msg, HeaderHelper header) {
        pubSub.sendMessage(msg, header);
    }
}
