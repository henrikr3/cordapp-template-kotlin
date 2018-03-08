package com.template

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

/**
 * Demonstration of how to use the CordaRPCClient to connect to a Corda Node and
 * stream the contents of the node's vault.
 */
fun main(args: Array<String>) {
    TemplateClient().main(args)
}

private class TemplateClient {
    companion object {
        val logger: Logger = loggerFor<TemplateClient>()
        private fun logState(state: StateAndRef<TemplateState>) = logger.info("{}", state.state.data)
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: TemplateClient <node address>" }
        val nodeAddress = parse(args[0])
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.template.MainKt file.
        val proxy = client.start("user1", "test").proxy


        val flow = proxy.startFlow(::Initiator, proxy.partiesFromName("O=PartyB,=New York,C=US", true).first()).returnValue.getOrThrow()

//        flow.returnValue.getOrThrow()

        // Grab all existing TemplateStates and all future TemplateStates.
//        val (snapshot, updates) = proxy.vaultTrack(TemplateState::class.java)
//
//        // Log the existing TemplateStates and listen for new ones.
//        snapshot.states.forEach { logState(it) }
//        updates.toBlocking().subscribe { update ->
//            update.produced.forEach { logState(it) }
//        }
    }
}
