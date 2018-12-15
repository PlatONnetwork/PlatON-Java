package org.platon.p2p.pubsub;


import org.platon.p2p.plugins.RoutingTable;
import org.platon.p2p.router.MessageRouter;
import org.platon.p2p.session.SessionNotify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubsubMock extends PubSub{
    private static Logger logger = LoggerFactory.getLogger(PubsubMock.class);



    public PubsubMock(RoutingTable routingTableMock, MessageRouter messageRouter){
        this.pubSubRouter = new PubSubRouterMock(routingTableMock);
        this.setRoutingTable(routingTableMock);
        this.setMessageRouter(messageRouter);
        this.pubSubRouter.setMessageRouter(messageRouter);
        this.pubSubRouter.attach(this);
        //logger.debug("add sessionNotify");
        try {
            SessionNotify.addListener(new PubSubSessionNotify(this, routingTableMock.getLocalNode()));
        } catch (Exception e) {
            //logger.error(e);
        }
    }




}
