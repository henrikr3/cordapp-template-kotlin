package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.serialization.SerializationWhitelist
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

// *****************
// * API Endpoints *
// *****************
@Path("template")
class TemplateApi(val rpcOps: CordaRPCOps) {
    // Accessible at /api/template/templateGetEndpoint.
    @GET
    @Path("templateGetEndpoint")
    @Produces(MediaType.APPLICATION_JSON)
    fun templateGetEndpoint(): Response {
        return Response.ok("Template GET endpoint.").build()
    }
}

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class Initiator(val them: Party) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {

        val session = initiateFlow(them)

        session.send(21)

        val thirtySeven = session.receive(Integer::class.java).unwrap { it as Int }

        requireThat {
            "37 is 37" using (thirtySeven == 37)
        }

        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        // We create the transaction components.
        val outputState = NewTemplateState(StateDataExtra("pręt"), ourIdentity, them, "gg")
        val cmd = Command(NewTemplateContract.Commands.Issue(), listOf(ourIdentity.owningKey, them.owningKey))

        // We create a transaction builder and add the components.
        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(outputState, NEW_TEMPLATE_CONTRACT_ID)
                .addCommand(cmd)

        // We sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        val verySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(session)))

        // We finalise the transaction.
        subFlow(FinalityFlow(verySignedTx))
    }
}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        val twentyOne = counterpartySession.receive(Integer::class.java).unwrap { it as Int }

        requireThat {
           "21 is 21" using (twentyOne == 21)
        }

        counterpartySession.send(37)

        val signTransactionFlow = object : SignTransactionFlow(counterpartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must contain NewTemplateState." using (output is NewTemplateState)
                val iou = output as NewTemplateState
                "Template state contains only one ę." using (iou.data.s.count { it == 'ę' } == 1)
            }
        }

        subFlow(signTransactionFlow)
    }
}

@InitiatingFlow
@StartableByRPC
class Nullify(val stateRef: StateRef) : FlowLogic<Unit>() {

    object NULLING : ProgressTracker.Step("Nulling")

    override val progressTracker = ProgressTracker(NULLING)

    @Suspendable
    override fun call() {

        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val toNull: TransactionState<NewTemplateState> = serviceHub.loadState(stateRef) as TransactionState<NewTemplateState>

        // We create the transaction components.
        val outputState = NullifyConfirmation(toNull.data.data.s, toNull.data.me)
        val cmd = Command(NullifyContract.Commands.Null(), ourIdentity.owningKey)

        // We create a transaction builder and add the components.
        val txBuilder = TransactionBuilder(notary = notary)
                .addInputState(StateAndRef(toNull, stateRef))
                .addOutputState(outputState, NULLIFY_CONTRACT_ID)
                .addCommand(cmd)

        progressTracker.currentStep = NULLING

        // We sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // We finalise the transaction.
        subFlow(FinalityFlow(signedTx))
    }
}

// ***********
// * Plugins *
// ***********
class TemplateWebPlugin : WebServerPluginRegistry {
    // A list of classes that expose web JAX-RS REST APIs.
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::TemplateApi))
    //A list of directories in the resources directory that will be served by Jetty under /web.
    // This template's web frontend is accessible at /web/template.
    override val staticServeDirs: Map<String, String> = mapOf(
            // This will serve the templateWeb directory in resources to /web/template
            "template" to javaClass.classLoader.getResource("templateWeb").toExternalForm()
    )
}

// Serialization whitelist.
class TemplateSerializationWhitelist : SerializationWhitelist {
    override val whitelist: List<Class<*>> = listOf(TemplateData::class.java)
}

// This class is not annotated with @CordaSerializable, so it must be added to the serialization whitelist, above, if
// we want to send it to other nodes within a flow.
data class TemplateData(val payload: String)
