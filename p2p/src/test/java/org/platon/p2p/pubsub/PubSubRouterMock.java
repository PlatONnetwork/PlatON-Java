package org.platon.p2p.pubsub;

import org.platon.p2p.plugins.RoutingTable;

public class PubSubRouterMock extends PubSubRouter{

    RoutingTable routingTableMock = null;
    public PubSubRouterMock(RoutingTable routingTableMock) {
        this.routingTableMock = routingTableMock;
        setRoutingTable(routingTableMock);
    }

}
