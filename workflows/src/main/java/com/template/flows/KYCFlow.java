package com.template.flows;

import java.security.PublicKey;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.template.contracts.KYCContract;
import com.template.states.KYCState;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.flows.CollectSignaturesFlow;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import static java.util.Arrays.asList;

@InitiatingFlow
@StartableByRPC
public class KYCFlow extends FlowLogic<Void> {
    private final String attachmentHash;
    private final Party targetBank;

    private final ProgressTracker progressTracker = new ProgressTracker();

    public KYCFlow(String attachmentHash, Party targetBank) {
        this.attachmentHash = attachmentHash;
        this.targetBank = targetBank;
    }

    public String getAttachmentHash() {
        return attachmentHash;
    }

    public Party getTargetBank() {
        return targetBank;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Void call()
            throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        KYCState outputState = new KYCState(getOurIdentity(), targetBank, attachmentHash);
        List<PublicKey> requiredSigners = asList(getOurIdentity().getOwningKey(), targetBank.getOwningKey());
        Command command = new Command<>(new KYCContract.Create(), requiredSigners);

        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, KYCContract.ID)
                .addCommand(command);
        txBuilder.verify(getServiceHub());
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);
        FlowSession toBankSession = initiateFlow(targetBank);
        SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                signedTx, ImmutableList.of(toBankSession), CollectSignaturesFlow.tracker()
        ));
        subFlow(new FinalityFlow(fullySignedTx, toBankSession));
        return null;
    }
}
