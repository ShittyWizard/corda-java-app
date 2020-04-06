package com.template.web;

import org.springframework.stereotype.Component;

import com.template.states.KYCState;
import com.template.web.dto.KYCStateResource;

import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;

@Component
public class KYCStateResourceAssembler {
    public KYCStateResource toResource(KYCState kycState) {
        KYCStateResource resource = new KYCStateResource();

        String partyNameSourceBank = getNameByParty(kycState.getSourceBank());
        resource.setPartyNameSourceBank(partyNameSourceBank);

        String partyNameTargetBank = getNameByParty(kycState.getTargetBank());
        resource.setPartyNameTargetBank(partyNameTargetBank);

        resource.setHashOfFile(kycState.getHashOfFile());
        resource.setCreateTs(kycState.getCreateTs());

        return resource;
    }

    private String getNameByParty(Party party) {
        CordaX500Name name = party.getName();
        return String.format("%s:%s, %s", name.getCountry(), name.getLocality(), name.getOrganisation());
    }
}
