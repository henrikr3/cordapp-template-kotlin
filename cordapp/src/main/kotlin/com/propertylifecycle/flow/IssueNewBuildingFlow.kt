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
import net.corda.core.crypto.toStringShort

/**
 * This is the flow which handles issuance of new buildings on the ledger.
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IssueNewBuildingFlow(val state: BuildingState) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = this.serviceHub.networkMapCache.notaryIdentities.single()

        val transactionBuilder = TransactionBuilder(notary).withItems(
                Command(BuildingContract.Commands.IssueNewBuilding(), state.participants.map { it.owningKey }),
                StateAndContract(state, BuildingContract.CONTRACT_ID))

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessionsToCollectFrom = (state.participants - ourIdentity).asSequence().map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(signedTransaction, sessionsToCollectFrom))

        return subFlow(FinalityFlow(stx))
    }
}

/**
 * This is the flow which signs new building issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IssueNewBuildingFlow::class)
class IssueNewBuildingFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a building transaction" using (output is BuildingState)
            }
        }
        subFlow(signedTransactionFlow)
    }
}