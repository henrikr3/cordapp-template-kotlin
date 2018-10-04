package net.corda.training.contract

import com.propertylifecycle.*
import com.propertylifecycle.contract.BuildingContract
import com.propertylifecycle.state.BuildingState
import net.corda.core.contracts.*
import net.corda.finance.*
import net.corda.testing.contracts.DummyState
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.*

/**
 * Practical exercise instructions for Contracts Part 1.
 * The objective here is to write some contract code that verifies a transaction to issue an [BuildingState].
 * As with the [BuildingStateTests] uncomment each unit test and run them one at a time. Use the body of the tests and the
 * task description to determine how to get the tests to pass.
 */
class IssueNewBuildingTests {
    // A pre-defined dummy command.
    class DummyCommand : TypeOnlyCommandData()
    private var ledgerServices = MockServices(listOf("com.propertylifecycle"))
    private val TestBuilding125k = BuildingState(TEST_ADDRESS1, 125000.POUNDS, TEST_BUILDER.party, null, null)
    private val TestBuilding0k = BuildingState(TEST_ADDRESS1, 0.POUNDS, TEST_BUILDER.party, null, null)
    private val TestBuilding5kUSD = BuildingState(TEST_ADDRESS1, 5000.DOLLARS, TEST_BUILDER.party, null, null)
    private val TestBuilding10kCHF = BuildingState(TEST_ADDRESS1, 10000.SWISS_FRANCS, TEST_BUILDER.party, null, null)
    private val TestBuildingWithSeller = BuildingState(TEST_ADDRESS1, 5000.DOLLARS, TEST_BUILDER.party, TEST_OWNER1.party, null)
    private val TestBuildingWithBuyer = BuildingState(TEST_ADDRESS1, 5000.DOLLARS, TEST_BUILDER.party, null, TEST_OWNER2.party)

    /**
     * Test to ensure IssueNewBuilding is working.
     */
    @Test
    fun mustIncludeIssueCommand() {
        ledgerServices.ledger {
            transaction {
                output(BuildingContract.CONTRACT_ID,  TestBuilding125k)
                command(listOf(TEST_AGENCY.publicKey, TEST_OWNER1.publicKey), DummyCommand()) // Wrong type.
                this.fails()
            }
            transaction {
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding()) // Correct type.
                this.verifies()
            }
        }
    }

    /**
     * Issue transactions should not have any input state references. Therefore we must check to
     * ensure that no input states are included in a transaction to issue an TestBuilding125k.
     */
    @Test
    fun issueTransactionMustHaveNoInputs() {
        ledgerServices.ledger {
            transaction {
                input(BuildingContract.CONTRACT_ID, DummyState())
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                this `fails with` "No inputs should be consumed when creating a new building."
            }
            transaction {
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                this.verifies() // As there are no input states.
            }
        }
    }

    /**
     * Now we need to ensure that only one [BuildingState] is issued per transaction.
     */
    @Test
    fun issueTransactionMustHaveOneOutput() {
        ledgerServices.ledger {
            transaction {
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding125k) // Two outputs fails.
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                this `fails with` "Only one output state should be created when creating a new building."
            }
            transaction {
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding125k) // One output passes.
                this.verifies()
            }
        }
    }

    /**
     * Now we need to consider the properties of the [BuildingState].
     * We need to ensure that an TestBuilding125k should always have a positive value.
     */
    @Test
    fun cannotCreateZeroValueBuildings() {
        ledgerServices.ledger {
            transaction {
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding0k) // Zero amount fails.
                this `fails with` "A new building must have a positive value."
            }
            transaction {
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding10kCHF)
                this.verifies()
            }
            transaction {
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                this.verifies()
            }
            transaction {
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding5kUSD)
                this.verifies()
            }
        }
    }

    /**
     * For obvious reasons, there cannot be a buyer nor a seller.
     */
    @Test
    fun lenderAndBorrowerCannotBeTheSame() {
        ledgerServices.ledger {
            transaction {
                command(listOf(TEST_BUILDER.publicKey),BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuildingWithBuyer)
                this `fails with` "There should be no buyer."
            }
            transaction {
                command(listOf(TEST_BUILDER.publicKey),BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuildingWithSeller)
                this `fails with` "There should be no seller."
            }
            transaction {
                command(listOf(TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                this.verifies()
            }
        }
    }

    /**
     * The list of public keys which the commands hold should contain all of the participants defined in the [BuildingState].
     * This is because the TestBuilding125k is a bilateral agreement where both parties involved are required to sign to issue an
     * TestBuilding125k or change the properties of an existing TestBuilding125k.
     * TODO: Add a contract constraint to check that all the required signers are [BuildingState] participants.
     * Hint:
     * - In Kotlin you can perform a set equality check of two sets with the == operator.
     * - We need to check that the signers for the transaction are a subset of the participants list.
     * - We don't want any additional public keys not listed in the Buildings participants list.
     * - You will need a reference to the Issue command to get access to the list of signers.
     * - [requireSingleCommand] returns the single required command - you can assign the return value to a constant.
     *
     * Kotlin Hints
     * Kotlin provides a map function for easy conversion of a [Collection] using map
     * - https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/map.html
     * [Collection] can be turned into a set using toSet()
     * - https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/to-set.html
     */
    @Test
    fun lenderAndBorrowerMustSignIssueTransaction() {
        ledgerServices.ledger {
            transaction {
                command(TEST_OWNER1.publicKey, BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                this `fails with` "Check participants have signed."
            }
            transaction {
                command(listOf(TEST_AGENCY.publicKey, TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                this `fails with` "Check participants have signed."
            }
            transaction {
                command(listOf(TEST_BUILDER.publicKey, TEST_BUILDER.publicKey, TEST_BUILDER.publicKey, TEST_BUILDER.publicKey), BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                this.verifies()
            }
            transaction {
                command(listOf(TEST_BUILDER.publicKey),BuildingContract.Commands.IssueNewBuilding())
                output(BuildingContract.CONTRACT_ID, TestBuilding125k)
                this.verifies()
            }
        }
    }
}
