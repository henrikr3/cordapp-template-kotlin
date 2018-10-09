package com.propertylifecycle.contract

import com.propertylifecycle.*
import com.propertylifecycle.state.BuildingState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.finance.POUNDS
import net.corda.finance.SWISS_FRANCS
import net.corda.testing.contracts.DummyState
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

/**
 * Tests to ensure that the [BuildingState] changes as expected when performing [TransferToNewOwner] command.
 */
class TransferToNewOwnerTests {
    // A pre-defined dummy command.
    class DummyCommand : TypeOnlyCommandData()

    private var ledgerServices = MockServices(listOf("com.propertylifecycle"))
    private val TestBuilding125k = BuildingState(TEST_ADDRESS1, 125000.POUNDS, TEST_BUILDER.party, null)
    private val TestBuilding125kToOwner1 = BuildingState(TEST_ADDRESS1, 125000.POUNDS, TEST_OWNER1.party, TEST_BUILDER.party)
    private val TestBuilding125kToOwner1WithNoValue = BuildingState(TEST_ADDRESS1, 0.POUNDS, TEST_OWNER1.party, TEST_BUILDER.party)
    private val TestBuilding125kToOwner1UsingCHF = BuildingState(TEST_ADDRESS1, 115000.SWISS_FRANCS, TEST_OWNER1.party, TEST_BUILDER.party)

    /**
     * Test to ensure TransferToNewOwner is working.
     */
    @Test
    fun mustIncludeTransferCommand() {
        ledgerServices.ledger {
            transaction {
                input(BuildingContract.CONTRACT_ID,  TestBuilding125k)
                output(BuildingContract.CONTRACT_ID,  TestBuilding125kToOwner1)
                command(listOf(TEST_AGENCY.publicKey, TEST_OWNER1.publicKey), DummyCommand()) // Wrong type.
                this.fails()
            }
            transaction {
                input(BuildingContract.CONTRACT_ID,  TestBuilding125k)
                output(BuildingContract.CONTRACT_ID,  TestBuilding125kToOwner1)
                command(listOf(TEST_BUILDER.publicKey, TEST_OWNER1.publicKey), BuildingContract.Commands.TransferToNewOwner()) // Correct type.
                this.verifies()
            }
        }
    }

    /**
     * Issue transactions should not have any input state references. Therefore we must check to
     * ensure that no input states are included in a transaction to issue an TestBuilding125k.
     */
    @Test
    fun issueTransactionMustHaveOneInputAndOneOutput() {
        ledgerServices.ledger {
            transaction {
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.TransferToNewOwner())
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                this `fails with` "A building transfer transaction must consume one input state."
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, DummyState())
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this `fails with` "A building transfer transaction must create one output state."
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1)
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this `fails with` "A building transfer transaction must consume one input state."
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1)
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this `fails with` "A building transfer transaction must create one output state."
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1)
                command(listOf(TEST_BUILDER.publicKey, TEST_OWNER1.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this.verifies()
            }
        }
    }

    /**
     * Now we need to consider the properties of the [BuildingState].
     * We need to ensure that an TestBuilding125k should always have a positive value.
     */
    @Test
    fun ensureBuildingValueRemainsPositive() {
        ledgerServices.ledger {
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1WithNoValue)
                command(listOf(TEST_BUILDER.publicKey, TEST_OWNER1.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this `fails with` "A building must have a positive value."
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1UsingCHF)
                command(listOf(TEST_BUILDER.publicKey, TEST_OWNER1.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this.verifies()
            }
        }
    }

    /**
     * Ensure buyer and seller and new owner are set correctly.
     */
    @Test
    fun ensureBuyerSellerAndOwnerChangeCorrectly() {
        val TestBuilding125kToOwner1WithWrongBuyer = BuildingState(TEST_ADDRESS1, 125000.POUNDS, TEST_OWNER2.party, TEST_OWNER1.party)
        val TestBuilding125kToOwner1WithWrongSeller = BuildingState(TEST_ADDRESS1, 125000.POUNDS, TEST_OWNER2.party, TEST_BUILDER.party)
        ledgerServices.ledger {
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                command(listOf(TEST_BUILDER.publicKey, TEST_OWNER1.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this `fails with` "Owner property should change."
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1WithWrongBuyer)
                command(listOf(TEST_BUILDER.publicKey, TEST_OWNER1.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this `fails with` "Buyer property should change."
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1WithWrongSeller)
                command(listOf(TEST_BUILDER.publicKey, TEST_OWNER1.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this `fails with` "Seller property should change."
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1UsingCHF)
                command(listOf(TEST_BUILDER.publicKey, TEST_OWNER1.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this.verifies()
            }
        }
    }

    /**
     * The list of public keys which the commands hold should contain all of the participants defined in the [BuildingState].
     * This is because the building transfer is a bilateral agreement where both parties involved are required to
     * sign to transfer the ownership of the building.
     */
    @Test
    fun checkThatBuyerSellerAndOwnerMustSign() {
        ledgerServices.ledger {
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1)
                command(TEST_OWNER1.publicKey, BuildingContract.Commands.TransferToNewOwner())
                this `fails with` "The buyer, old owner and seller must sign the building transfer transaction."
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1)
                command(listOf(TEST_AGENCY.publicKey, TEST_BUILDER.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this `fails with` "The buyer, old owner and seller must sign the building transfer transaction."
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1)
                command(listOf(TEST_BUILDER.publicKey, TEST_BUILDER.publicKey, TEST_BUILDER.publicKey, TEST_OWNER1.publicKey), BuildingContract.Commands.TransferToNewOwner())
                this.verifies()
            }
            transaction {
                input(BuildingContract.CONTRACT_ID, TestBuilding125k)
                output(BuildingContract.CONTRACT_ID, TestBuilding125kToOwner1)
                command(listOf(TEST_BUILDER.publicKey, TEST_OWNER1.publicKey),BuildingContract.Commands.TransferToNewOwner())
                this.verifies()
            }
        }
    }
}
