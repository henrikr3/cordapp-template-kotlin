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
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import java.security.PublicKey

/**
 * This is the flow which handles issuance of new buildings on the ledger.
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class TransferToNewOwnerFlow(val linearId: UniqueIdentifier, val newOwner: Party) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val stateAndRef = serviceHub.vaultService.queryBy<BuildingState>(queryCriteria).states.single()
        val previousState = stateAndRef.state.data
        val stateWithNewOwner: BuildingState = previousState.transferToNewOwner(newOwner)

        if(previousState.owner != ourIdentity) {
            throw java.lang.IllegalArgumentException("Only the current owner can execute this flow!")
        }
        val notary = this.serviceHub.networkMapCache.notaryIdentities.single()

        val signers: Sequence<Party>
        val participants: Sequence<Party>
        if(previousState.seller != null) {
            signers = (previousState.participants - previousState.seller + newOwner).asSequence()
            participants = (previousState.participants - previousState.owner - previousState.seller + newOwner).asSequence()
        } else {
            signers = (previousState.participants + newOwner).asSequence()
            participants = (previousState.participants - previousState.owner + newOwner).asSequence()
        }

        val transactionBuilder = TransactionBuilder(notary).withItems(
                stateAndRef,
                Command(BuildingContract.Commands.TransferToNewOwner(), signers.map { it.owningKey }.toList()),
                StateAndContract(stateWithNewOwner, BuildingContract.CONTRACT_ID))

        transactionBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessionsToCollectFrom = participants.map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(signedTransaction, sessionsToCollectFrom))

        return subFlow(FinalityFlow(stx))
    }
}

/**
 * This is the flow which signs new building issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(TransferToNewOwnerFlow::class)
class TransferToNewOwnerFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
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