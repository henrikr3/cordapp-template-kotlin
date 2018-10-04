package com.propertylifecycle.contract

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import com.propertylifecycle.state.BuildingState
/*
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.utils.sumCash
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.identity.Party
import net.corda.finance.contracts.asset.CASH
import net.corda.core.internal.signWithCert
import java.util.*
import net.corda.finance.utils.sumCash
*/

/**
 * This is where you'll add the contract code which defines how the [BuildingState] behaves.
 */
class BuildingContract : Contract {
    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.propertylifecycle.contract.BuildingContract"
    }

    /**
     * Add any commands required for this contract as classes within this interface.
     * It is useful to encapsulate your commands inside an interface, so you can use the [requireSingleCommand]
     * function to check for a number of commands which implement this interface.
     */
    interface Commands : CommandData {
        class IssueNewBuilding : TypeOnlyCommandData(), Commands
        class TransferToNewOwner : TypeOnlyCommandData(), Commands
        class ListBuildingForSale : TypeOnlyCommandData(), Commands
    }

    /**
     * The contract code for the [BuildingContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.IssueNewBuilding -> requireThat{
                "No inputs should be consumed when creating a new building." using (tx.inputs.size == 0)
                "Only one output state should be created when creating a new building." using (tx.outputs.size == 1)
                val state = tx.outputStates.single() as BuildingState
                "A new building must have a positive value." using (state.value.quantity > 0)
                "There should be no seller." using (state.seller == null)
                "There should be no buyer." using (state.buyer == null)
                "Check participants have signed." using
                        (command.signers.toSet() == state.participants.map { it.owningKey }.toSet())
            }

            is Commands.TransferToNewOwner -> requireThat{
                "A building transfer transaction must consume one input state." using (tx.inputs.size == 1)
                "A building transfer transaction must create one output state." using (tx.outputs.size == 1)
                val inputState = tx.inputStates.single() as BuildingState
                val outputState = tx.outputStates.single() as BuildingState
                "Address should not change." using (outputState.address.equals(outputState.address))
                "Owner property should change." using (!inputState.owner.equals(outputState.owner))
                "Buyer cannot be null." using (outputState.buyer != null)
                "Buyer property should change." using (!outputState.buyer!!.equals(inputState.buyer))
                "Seller cannot be null." using (outputState.seller != null)
                "Seller property should change." using (!outputState.seller!!.equals(inputState.seller))
                "Seller should be previous owner." using (inputState.owner.equals(outputState.seller))
                "Owner should be new owner." using (outputState.owner.equals(outputState.buyer))

                "A building must have a positive value." using (outputState.value.quantity > 0)

                "The buyer, old owner and seller must sign the building transfer transaction." using
                        (command.signers.toSet() == (inputState.participants.map { it.owningKey }.toSet() `union`
                                outputState.participants.map { it.owningKey }.toSet()))
            }

            is Commands.ListBuildingForSale -> requireThat{
/*                val ious = tx.groupStates<BuildingState, UniqueIdentifier> { it.linearId }.single()
                "There must be one input IOU." using (ious.inputs.size == 1)
                val cash = tx.outputsOfType(Cash.State::class.java)
                "There must be output cash." using (cash.isNotEmpty())
                val input = ious.inputs.single()
                val lenderCash = cash.filter { it.owner == input.lender }
                "There must be output cash paid to the recipient." using (lenderCash.isNotEmpty())

                val cashLeftToBePaid = input.amount - input.paid
                val totalLenderCash = lenderCash.sumCash().withoutIssuer()
                "The amount settled cannot be more than the amount outstanding." using (cashLeftToBePaid >= totalLenderCash)
                "Token mismatch: GBP vs USD" using (input.paid.token == input.amount.token)

                if(cashLeftToBePaid == totalLenderCash) {
                    "There must be no output IOU as it has been fully settled." using (ious.outputs.size == 0)
                } else {
                    "There must be one output IOU." using (ious.outputs.size == 1)
                    val inputState = ious.inputs.single() as BuildingState
                    val outputState = ious.outputs.single() as BuildingState
                    "The borrower may not change when settling." using (inputState.borrower == outputState.borrower)
                    "The amount may not change when settling." using (inputState.amount == outputState.amount)
                    "The lender may not change when settling." using (inputState.lender == outputState.lender)
                    val inputStateCopy = inputState.copy(
                            amount = inputState.amount,
                            lender = inputState.lender,
                            borrower = inputState.borrower,
                            paid = outputState.paid,
                            linearId = inputState.linearId)
                    "Only the paid property may change." using (inputStateCopy == outputState)

                    "Both lender and borrower together only must sign IOU settle transaction." using
                            (command.signers.toSet() == (inputState.participants.map { it.owningKey }.toSet() `union`
                                    outputState.participants.map { it.owningKey }.toSet()))
                }*/
            }
        }
    }
}
