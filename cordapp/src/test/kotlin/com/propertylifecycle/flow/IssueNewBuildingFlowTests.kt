package com.propertylifecycle.flow

import com.propertylifecycle.TEST_ADDRESS1
import com.propertylifecycle.TEST_BUILDER
import com.propertylifecycle.TEST_OWNER1
import com.propertylifecycle.TEST_OWNER2
import com.propertylifecycle.contract.BuildingContract
import com.propertylifecycle.state.BuildingState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.finance.DOLLARS
import net.corda.finance.POUNDS
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Note! These tests rely on Quasar to be loaded, set your run configuration to "-ea -javaagent:lib/quasar.jar"
 */
class IssueNewBuildingFlowTests {
    private lateinit var mockNetwork: MockNetwork
    private lateinit var builderNode: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("com.propertylifecycle"),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        builderNode = mockNetwork.createNode(MockNodeParameters(legalName = TEST_BUILDER.name))
        val startedNodes = arrayListOf(builderNode)

        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach { it.registerInitiatedFlow(IssueNewBuildingFlowResponder::class.java) }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    /**
     * Check that [IssueNewBuildingFlow] returns a partially signed flow.
     */
    @Test
    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
        val builder = builderNode.info.chooseIdentityAndCert().party
        val testBuilding125k = BuildingState(TEST_ADDRESS1, 125000.POUNDS, builder, null)
        val flow = IssueNewBuildingFlow(testBuilding125k)
        val future = builderNode.startFlow(flow)
        mockNetwork.runNetwork()

        // Return the unsigned(!) SignedTransaction object from the IssueNewBuildingFlow.
        val ptx: SignedTransaction = future.getOrThrow()
        // Print the transaction for debugging purposes.
        println(ptx.tx)

        // Check the transaction is well formed...
        // No outputs, one input BuildingState and a command with the right properties.
        assert(ptx.tx.inputs.isEmpty())
        assert(ptx.tx.outputs.single().data is BuildingState)
        val command = ptx.tx.commands.single()
        assert(command.value is BuildingContract.Commands.IssueNewBuilding)
        assert(command.signers.toSet() == testBuilding125k.participants.map { it.owningKey }.toSet())
        ptx.verifySignaturesExcept(builder.owningKey,
                mockNetwork.defaultNotaryNode.info.legalIdentitiesAndCerts.first().owningKey)
    }

    /**
     * Check that the flow passes the [BuildingContract] verifications.
     */
    @Test
    fun flowReturnsVerifiedPartiallySignedTransaction() {
        // Check that [BuildingState] with zero value fails.
        val builder = builderNode.info.chooseIdentityAndCert().party
        val testBuilding0k = BuildingState(TEST_ADDRESS1, 0.POUNDS, builder, null)
        val future1 = builderNode.startFlow(IssueNewBuildingFlow(testBuilding0k))
        mockNetwork.runNetwork()
        assertFailsWith<TransactionVerificationException> { future1.getOrThrow() }

        // Check that [BuildingState] with a seller fails.
        val testBuildingWithSeller = BuildingState(TEST_ADDRESS1, 5000.DOLLARS, builder, TEST_OWNER1.party)
        val future2 = builderNode.startFlow(IssueNewBuildingFlow(testBuildingWithSeller))
        mockNetwork.runNetwork()
        assertFailsWith<TransactionVerificationException> { future2.getOrThrow() }

        // Check that a valid [BuildingState] passes.
        val testBuilding125k = BuildingState(TEST_ADDRESS1, 125000.POUNDS, builder, null)
        val future4 = builderNode.startFlow(IssueNewBuildingFlow(testBuilding125k))
        mockNetwork.runNetwork()
        future4.getOrThrow()
    }

    /**
     * Ensure that the flow is signed and verified correctly.
     */
    @Test
    fun flowReturnsTransactionSignedByAndSignaturesVerified() {
        val builder = builderNode.info.chooseIdentityAndCert().party
        val testBuilding125k = BuildingState(TEST_ADDRESS1, 125000.POUNDS, builder, null)
        val flow = IssueNewBuildingFlow(testBuilding125k)
        val future = builderNode.startFlow(flow)
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        stx.verifyRequiredSignatures()
    }

    /**
     * Now we need to check that the finished [SignedTransaction] was properly stored in the vault.
     */
    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {
        val builder = builderNode.info.chooseIdentityAndCert().party
        val testBuilding125k = BuildingState(TEST_ADDRESS1, 125000.POUNDS, builder, null)
        val flow = IssueNewBuildingFlow(testBuilding125k)
        val future = builderNode.startFlow(flow)
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        println("Signed transaction hash: ${stx.id}")

        val validatedTransactions = listOf(builderNode).map { it.services.validatedTransactions.getTransaction(stx.id)}
        assertEquals(1, validatedTransactions.size)
        validatedTransactions.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${stx.id}")
            assertEquals(stx.id, txHash)
        }
    }
}
