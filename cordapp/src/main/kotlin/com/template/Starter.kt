package com.template

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import kotlin.system.exitProcess

/**
 * Demonstration of how to use the CordaRPCClient to connect to a Corda Node and
 * stream the contents of the node's vault.
 */
fun main(args: Array<String>) {
    TemplateClientStarter().main(args)
}

private class TemplateClientStarter {
    companion object {
        val logger: Logger = loggerFor<TemplateClientStarter>()
        private fun logState(state: StateAndRef<TemplateState>) = logger.info("{}", state.state.data)
    }

    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: TemplateClient <node address> party_name" }
        val nodeAddress = parse(args[0])
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.template.MainKt file.
        val proxy = client.start("user1", "test").proxy

        val startFlow = proxy.startFlow(::Initiator, proxy.partiesFromName(args[1], false).first())

        logger.info("Started {}", startFlow.id)

        startFlow.returnValue.getOrThrow()


        startFlow.returnValue.then { f ->
            logger.info("Returned: {}", f)
            exitProcess(0)
        }
    }
}
