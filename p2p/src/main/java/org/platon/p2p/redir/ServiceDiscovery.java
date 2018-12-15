package org.platon.p2p.redir;

import org.platon.p2p.common.HeaderHelper;
import org.platon.p2p.handler.PlatonMessageType;
import org.platon.p2p.proto.redir.ReDiRFindMessage;
import org.platon.p2p.proto.redir.ReDiRFindRespMessage;
import org.platon.p2p.proto.redir.ReDiRMessage;
import org.platon.p2p.proto.redir.ReDiRRespMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yangzhou
 * @create 2018-04-27 11:53
 */
@Component("serviceDiscovery")
public class ServiceDiscovery {

    @Autowired
    ServiceDiscoveryManager serviceDiscoveryManager;

    public void setServiceDiscoveryManager(ServiceDiscoveryManager serviceDiscoveryManager) {
        this.serviceDiscoveryManager = serviceDiscoveryManager;
    }

    @PlatonMessageType("ReDiRMessage")
    public void publish(ReDiRMessage msg, HeaderHelper header) {
        serviceDiscoveryManager.publish(msg, header);
    }

    @PlatonMessageType("ReDiRRespMessage")
    public void publishResp(ReDiRRespMessage msg, HeaderHelper header) {
        serviceDiscoveryManager.publishResp(msg, header);
    }

    @PlatonMessageType("ReDiRFindMessage")
    public void discovery(ReDiRFindMessage msg, HeaderHelper header) {
        serviceDiscoveryManager.discovery(msg, header);
    }

    @PlatonMessageType("ReDiRFindRespMessage")
    public void discoveryResp(ReDiRFindRespMessage msg, HeaderHelper header) {
        serviceDiscoveryManager.discoveryResp(msg, header);
    }

}