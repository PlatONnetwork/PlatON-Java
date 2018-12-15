package org.platon.p2p.pubsub;

import org.platon.p2p.MessageHook;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangzhou
 * @create 2018-07-26 10:09
 */
public class PubSubMessageHook implements MessageHook.MessageCallback {
    private static Logger logger = LoggerFactory.getLogger(PubSubMessageHook.class);
    @Override
    public boolean isNeedProcess(PlatonMessage request) {
        logger.trace("PubSubMessageHook need process");

        return true;
    }
}
