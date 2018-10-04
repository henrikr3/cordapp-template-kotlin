package com.propertylifecycle

import com.propertylifecycle.state.Address
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity

val TEST_BUILDER = TestIdentity(CordaX500Name(organisation = "Bob the builder", locality = "London", country = "GB"))
val TEST_AGENCY = TestIdentity(CordaX500Name(organisation = "Super sellers", locality = "London", country = "GB"))
var TEST_OWNER1 = TestIdentity(CordaX500Name(organisation = "Charlie the first owner", locality = "London", country = "GB"))
val TEST_OWNER2 = TestIdentity(CordaX500Name(organisation = "John the magnate", locality = "New York", country = "US"))

val TEST_ADDRESS1 = Address(4, "Random street", "Random city", "GB")
val TEST_ADDRESS2 = Address(5, "Another street", "Another city", "GB")
