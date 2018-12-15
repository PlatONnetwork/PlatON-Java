package org.platon.p2p.common;

import org.platon.p2p.proto.common.NodeID;
import org.platon.p2p.proto.common.RoutableID;
import org.platon.p2p.proto.platon.Header;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yangzhou
 * @create 2018-08-20 14:44
 */
public class HeaderHelper {


    private Header header;


    public static HeaderHelper build(Header header) {
        return new HeaderHelper(header);
    }
    public HeaderHelper(Header header) {
        this.header = header;
    }

    public Header getHeader() {
        return header;
    }

    public List<RoutableID> destList() {
        return header.getDestList();
    }

    public List<NodeID> viaList() {
        return header.getViaList();
    }

    public String txId() {
        return header.getTxId();
    }

    public List<RoutableID> viaToDest() {
        List<RoutableID> routableIDList = new ArrayList<>();
        for (NodeID nodeId : header.getViaList()) {
            routableIDList.add(NodeUtils.toRoutableID(nodeId));
        }

        return routableIDList;
    }

    public NodeID senderId() {
        return header.getVia(0);
    }


}
