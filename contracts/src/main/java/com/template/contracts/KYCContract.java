package com.template.contracts;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.template.states.KYCState;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;

public class KYCContract implements Contract {
    public static final String ID = "com.template.contracts.KYCContract";

    public static class Create implements CommandData {

    }

    @Override
    public void verify(
            @NotNull
                    LedgerTransaction tx
    )
            throws IllegalArgumentException {
        final CommandWithParties<KYCContract.Create> createCommand = requireSingleCommand(tx.getCommands(), KYCContract.Create.class);

        if (!tx.getInputs().isEmpty()) {
            throw new IllegalArgumentException("Inputs should be empty");
        }

        if (tx.getOutputs().size() != 1) {
            throw new IllegalArgumentException(("Output should be the only"));
        }

        final KYCState output = tx.outputsOfType(KYCState.class).get(0);

        final Party fromBank = output.getSourceBank();
        final Party toBank = output.getTargetBank();
        if (output.getHashOfFile().isEmpty()) {
            throw new IllegalArgumentException("File should be attached");
        }
        if (fromBank == null || toBank == null) {
            throw new IllegalArgumentException("Banks should not be null");
        }

        final List<PublicKey> requiredSigners = createCommand.getSigners();
        final List<PublicKey> expectedSigners = Arrays.asList(fromBank.getOwningKey(), toBank.getOwningKey());

        if (requiredSigners.size() != 2) {
            throw new IllegalArgumentException("There must be two signers");
        }
        if (!requiredSigners.containsAll(expectedSigners)) {
            throw new IllegalArgumentException("The both banks ('from' and 'two') must be signers");
        }
    }
}
