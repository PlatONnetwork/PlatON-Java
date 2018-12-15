package org.platon.p2p.redir;

import org.platon.p2p.db.DB;
import org.platon.p2p.plugins.RoutingTable;
import org.platon.p2p.pubsub.PubSub;
import org.platon.p2p.router.MessageRouter;

public class ServiceDiscoverManagerMock extends ServiceDiscoveryManager {

    ServiceDiscoverManagerMock(PubSub pubSub, MessageRouter messageRouter, DB db, RoutingTable routingTable){
        this.pubSub = pubSub;
        this.messageRouter = messageRouter;
        this.db = db;
        this.reDiR = new ReDiR();
        this.routingTable = routingTable;


        reDiR.pubSub = pubSub;
        reDiR.db = db;
        reDiR.messageRouter = messageRouter;

    }

}
