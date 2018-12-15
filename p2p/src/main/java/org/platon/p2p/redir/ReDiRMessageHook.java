package org.platon.p2p.redir;

import org.platon.p2p.MessageHook;
import org.platon.p2p.common.Bytes;
import org.platon.p2p.common.NodeUtils;
import org.platon.p2p.common.ProtoBufHelper;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.platon.p2p.proto.redir.ReDiRFindMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yangzhou
 * @create 2018-08-06 11:10
 */
public class ReDiRMessageHook  implements MessageHook.MessageCallback {
    private static Logger logger = LoggerFactory.getLogger(ReDiRMessageHook.class);

    private ServiceDiscoveryManager serviceDiscoveryManager = null;
    ReDiRMessageHook(ServiceDiscoveryManager serviceDiscoveryManager){
        this.serviceDiscoveryManager = serviceDiscoveryManager;
    }

    @Override
    public boolean isNeedProcess(PlatonMessage request) {

        logger.trace("ReDiRMessageHook  process");

        try {

            List<RoutableID> destIdList = request.getHeader().getDestList();


            String topic = NodeUtils.getNodeIdString(destIdList.get(0).getId());





            if (serviceDiscoveryManager.pubSub.isSubscribe(topic)) {

                if (ProtoBufHelper.getTypeNameFromTypeUrl(
                        request.getBody().getData().getTypeUrl())
                        .compareTo(ProtoBufHelper.getFullName(ReDiRFindMessage.class)) == 0) {
                    return true;
                }

                Set<Bytes> peers = new HashSet<>();
                Set<Bytes> topicPeers = serviceDiscoveryManager.pubSub.listPeers(topic);
                if (topicPeers != null) {
                    peers.addAll(topicPeers);
                }

                Bytes localBytes = Bytes.valueOf(serviceDiscoveryManager.routingTable.getLocalNode().getId());
                peers.add(localBytes);


                if (NodeUtils.closestNode(Bytes.valueOf(destIdList.get(0).getId()), peers).compareTo(localBytes) == 0) {
                    logger.trace("need local process");
                    return true;
                }
            }

            logger.trace("doesn't need local process, is not subscribe topic:{}", topic);
            return false;
        }catch (Exception e) {
            logger.error("error:", e);
            return false;
        }

    }

}
