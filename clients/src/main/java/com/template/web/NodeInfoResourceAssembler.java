package com.template.web;

import org.springframework.stereotype.Component;

import com.template.web.dto.NodeInfoResource;

import net.corda.core.identity.Party;
import net.corda.core.node.NodeInfo;

@Component
public class NodeInfoResourceAssembler {
    public NodeInfoResource toResource(NodeInfo nodeInfo) {
        NodeInfoResource resource = new NodeInfoResource();
        resource.setAddress(nodeInfo.getAddresses().stream()
                                      .map(networkHostAndPort -> networkHostAndPort.getHost() + ":" + networkHostAndPort.getPort())
                                      .findFirst().orElse("no address"));
        resource.setPlatformVersion(nodeInfo.getPlatformVersion());
        resource.setSerial(nodeInfo.getSerial());
        resource.setPartyName(nodeInfo.getLegalIdentities().stream()
                                      .map(Party::getName)
                                      .map(name -> String.format("%s:%s, %s", name.getCountry(), name.getLocality(), name.getOrganisation()))
                                      .findFirst().orElse("no party"));
        return resource;
    }
}
