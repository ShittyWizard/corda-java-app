package com.template.flows;

import org.jetbrains.annotations.NotNull;

import com.template.states.KYCState;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.ReceiveFinalityFlow;
import net.corda.core.flows.SignTransactionFlow;
import net.corda.core.transactions.SignedTransaction;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(KYCFlow.class)
public class KYCFlowResponder extends FlowLogic<Void> {
    private final FlowSession anotherPartySession;

    public KYCFlowResponder(FlowSession anotherPartySession) {
        this.anotherPartySession = anotherPartySession;
    }

    @Suspendable
    @Override
    public Void call()
            throws FlowException {
        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession anotherPartySession) {
                super(anotherPartySession);
            }

            @Override
            protected void checkTransaction(
                    @NotNull
                            SignedTransaction stx
            )
                    throws FlowException {
                requireThat(r -> {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    r.using("This must be an KYC transaction.", output instanceof KYCState);
                    return  null;
                });
            }
        }
        SecureHash expectedTxId = subFlow(new SignTxFlow(anotherPartySession)).getId();

        subFlow(new ReceiveFinalityFlow(anotherPartySession, expectedTxId));

        return null;
    }
}
