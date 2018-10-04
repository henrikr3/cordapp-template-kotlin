package com.propertylifecycle.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import com.propertylifecycle.contract.BuildingContract
import com.propertylifecycle.state.BuildingState

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IssueNewBuildingFlow(val state: BuildingState) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Placeholder code to avoid type error when running the tests. Remove before starting the flow task!
        val notary = this.serviceHub.networkMapCache.notaryIdentities.single()
        val command = Command(BuildingContract.Commands.IssueNewBuilding(), state.participants.map { it.owningKey })

        val transactionBuilder1 = TransactionBuilder(notary)
        transactionBuilder1.addCommand(command)
        transactionBuilder1.addOutputState(state, BuildingContract.CONTRACT_ID)

        val transactionBuilder2 = TransactionBuilder(notary).withItems(command,
                StateAndContract(state, BuildingContract.CONTRACT_ID))

        transactionBuilder1.verify(serviceHub)
        transactionBuilder2.verify(serviceHub)
        val signedTransaction1 = serviceHub.signInitialTransaction(transactionBuilder1)
        val signedTransaction2 = serviceHub.signInitialTransaction(transactionBuilder2)
        requireThat { "Signed transaction creation methods should be identical" using
                (signedTransaction1.notary == signedTransaction2.notary &&
                        signedTransaction1.inputs == signedTransaction2.inputs) }

        val sessionsToCollectFrom = (state.participants - ourIdentity).asSequence().map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(signedTransaction2, sessionsToCollectFrom))

        return subFlow(FinalityFlow(stx))
    }
}

/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IssueNewBuildingFlow::class)
class IssueNewBuildingFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is BuildingState)
            }
        }
        subFlow(signedTransactionFlow)
    }
}