package org.platon.p2p.redir;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.platon.p2p.ForwardMessageHook;
import org.platon.p2p.common.Bytes;
import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.platon.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author yangzhou
 * @create 2018-07-26 18:57
 */
public class ReDiRForwardMessageHook implements ForwardMessageHook.ForwardMessageCallback {
    private static Logger logger = LoggerFactory.getLogger(ReDiRForwardMessageHook.class);

    private ServiceDiscoveryManager serviceDiscoveryManager = null;

    ReDiRForwardMessageHook(ServiceDiscoveryManager serviceDiscoveryManager) {
        this.serviceDiscoveryManager = serviceDiscoveryManager;
    }

    @Override
    public List<NodeID> nextHops(Header header, Any any) {
        logger.trace("receive nexthops message header:{}", header);
        List<NodeID> nextHops = new ArrayList<>();

        Set<Bytes> listPeers = serviceDiscoveryManager.pubSub.listPeers(NodeUtils.getNodeIdString(HeaderHelper.build(header).senderId().getId()));

        if (listPeers == null || listPeers.isEmpty()) {
            return null;
        }

        listPeers.forEach(node -> {
            nextHops.add(NodeID.newBuilder().setId(ByteString.copyFrom(node.getKey())).build());
        });

        return nextHops;
    }

}
