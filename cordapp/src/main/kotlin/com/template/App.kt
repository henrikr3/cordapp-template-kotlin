package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.SerializationWhitelist
import net.corda.core.transactions.TransactionBuilder
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

val PARTY_A_NAME = CordaX500Name("PartyA", "London", "GB")
val PARTY_B_NAME = CordaX500Name("PartyB", "London", "GB")
val PARTY_C_NAME = CordaX500Name("PartyC", "London", "GB")
val PARTY_D_NAME = CordaX500Name("PartyD", "London", "GB")

@InitiatingFlow
@StartableByRPC
class Initiator(val participants: List<Party>, val inputId: UniqueIdentifier? = null) : FlowLogic<UniqueIdentifier>() {
    @Suspendable
    override fun call(): UniqueIdentifier {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txBuilder = TransactionBuilder(notary)

        val linearId = if (inputId != null) {
            val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(inputId))
            val inputStateAndRef = serviceHub.vaultService.queryBy<TemplateState>(criteria).states.single()
            val outputState = TemplateState(participants, inputId)
            txBuilder
                    .addInputState(inputStateAndRef)
                    .addOutputState(outputState, TemplateContract.ID)
                    .addCommand(TemplateContract.Commands.Action(), ourIdentity.owningKey)
            inputId
        } else {
            val templateState = TemplateState(participants)
            txBuilder
                    .addOutputState(templateState, TemplateContract.ID)
                    .addCommand(TemplateContract.Commands.Action(), ourIdentity.owningKey)
            templateState.linearId
        }
        val stx = serviceHub.signInitialTransaction(txBuilder)
        subFlow(FinalityFlow(stx))
        return linearId
    }
}
