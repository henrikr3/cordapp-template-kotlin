package com.propertylifecycle.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
data class Address(val BuildingNumber: Int,
                   val StreetName: String,
                   val City: String,
                   val Country: String)
{
}

data class BuildingState(val address: Address,
                         val value: Amount<Currency>,
                         val owner: Party,
                         val seller: Party?,
                         val buyer: Party?,
                         override val linearId: UniqueIdentifier = UniqueIdentifier(id = UUID.randomUUID()))
    : LinearState {
    override val participants: List<Party> get() = listOfNotNull(owner, buyer, seller)

    public fun transferToNewOwner(newOwner: Party) : BuildingState {
        return this.copy(
                address = this.address,
                seller = this.owner,
                buyer = newOwner, // Is buyer field needed at all?
                owner = newOwner,
                value = this.value,
                linearId = this.linearId)
    }
}