package org.platon.p2p.pubsub;

import com.google.protobuf.ByteString;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.session.SessionNotify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangzhou
 * @create 2018-07-24 15:35
 */
public class PubSubSessionNotify implements SessionNotify.SessionNotifyCallback{
    private static Logger logger = LoggerFactory.getLogger(PubSubSessionNotify.class);

    private PubSub pubSub;

    private NodeID localNodeId;
    public PubSubSessionNotify(PubSub pubSub, NodeID localNodeId){
        this.pubSub = pubSub;
        this.localNodeId = localNodeId;
    }


    @Override
    public void create(ByteString remoteNodeId) {

        if (remoteNodeId.equals(localNodeId.getId())){
            return;
        }


        logger.debug("session on :{}",  NodeUtils.getNodeIdString(remoteNodeId));
        pubSub.addPeer(remoteNodeId);
    }

    @Override
    public void close(ByteString remoteNodeId) {
        if (remoteNodeId.equals(localNodeId.getId())){
            return;
        }
        logger.debug("session off :{}",  NodeUtils.getNodeIdString(remoteNodeId));
        pubSub.removePeer(remoteNodeId);
    }
}
