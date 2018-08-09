package com.template

import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlowTests {
    private val network = MockNetwork(listOf("com.template"))
    private val a = network.createNode(PARTY_A_NAME)
    private val b = network.createNode(PARTY_B_NAME)
    private val c = network.createNode(PARTY_C_NAME)
    private val d = network.createNode(PARTY_D_NAME)
    private val aParty = a.info.legalIdentities.first()
    private val bParty = b.info.legalIdentities.first()
    private val cParty = c.info.legalIdentities.first()
    private val dParty = d.info.legalIdentities.first()

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `dummy test`() {
        val future1 = a.startFlow(Initiator(listOf(aParty, bParty)))
        network.runNetwork()
        val linearId = future1.getOrThrow()

        val future2 = a.startFlow(Initiator(listOf(aParty, bParty, cParty), linearId))
        network.runNetwork()
        future2.getOrThrow()

        val future3 = c.startFlow(Initiator(listOf(cParty, dParty), linearId))
        network.runNetwork()
        future3.getOrThrow()

        a.transaction {
            println("\nAll unconsumed.")
            a.services.vaultService.queryBy<TemplateState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)).states.forEach {
                println(it)
            }
            println("\nAll consumed.")
            a.services.vaultService.queryBy<TemplateState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED)).states.forEach {
                println(it)
            }
            println("\nAll.")
            a.services.vaultService.queryBy<TemplateState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)).states.forEach {
                println(it)
            }
        }
    }
}