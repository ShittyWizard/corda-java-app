package com.template.states;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.template.contracts.KYCContract;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

@BelongsToContract(KYCContract.class)
public class KYCState implements ContractState {
    private final Date createTs;
    private final Party sourceBank;
    private final Party targetBank;
    private final String hashOfFile;

    public KYCState(Party sourceBank, Party targetBank, String hashOfFile) {
        this.sourceBank = sourceBank;
        this.targetBank = targetBank;
        this.hashOfFile = hashOfFile;
        this.createTs = new Date();
    }

    public Date getCreateTs() {
        return createTs;
    }

    public Party getSourceBank() {
        return sourceBank;
    }

    public Party getTargetBank() {
        return targetBank;
    }

    public String getHashOfFile() {
        return hashOfFile;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(sourceBank, targetBank);
    }
}
