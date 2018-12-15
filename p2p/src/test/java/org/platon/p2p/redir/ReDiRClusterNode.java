package org.platon.p2p.redir;

import org.platon.p2p.db.DB;
import org.platon.p2p.plugins.RoutingTableMock;
import org.platon.p2p.pubsub.PubSub;
import org.platon.p2p.pubsub.PubSubSessionNotify;
import org.platon.p2p.router.MessageRouterMock;


public class ReDiRClusterNode {
    ReDiRClusterNode(PubSub pubSub, DB db, MessageRouterMock messageRouter,
                     ServiceDiscoveryManager serviceDiscoveryManager,
                     PubSubSessionNotify notify, RoutingTableMock routingTable) {
        this.pubSub = pubSub;
        this.db = db;
        this.messageRouter = messageRouter;
        this.serviceDiscoveryManager = serviceDiscoveryManager;
        this.notify = notify;
        this.routingTable = routingTable;
    }
    PubSubSessionNotify notify;
    PubSub pubSub;
    DB db;
    MessageRouterMock messageRouter;
    ServiceDiscoveryManager serviceDiscoveryManager;
    RoutingTableMock routingTable;
}