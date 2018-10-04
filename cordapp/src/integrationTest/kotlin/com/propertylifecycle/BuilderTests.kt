package com.propertylifecycle

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.eclipse.jetty.http.HttpStatus
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals

class BuilderTests {
    private val builderIdentity = TestIdentity(CordaX500Name("Builder", "", "GB"))
    // region Utility functions

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
            DriverParameters(isDebug = true, startNodesInProcess = true)
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
            .map { startNode(providedName = it.name) }
            .waitForAll()

    // Starts multiple webservers simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startWebServers(vararg identities: TestIdentity) = startNodes(*identities)
            .map { startWebserver(it) }
            .waitForAll()

    // endregion

    @Test
    fun `name test`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (builderNode) = startNodes(builderIdentity)

        assertEquals(builderIdentity.name, builderNode.resolveName(builderIdentity.name))
    }

    @Test
    fun `builder api test`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        startWebServers(builderIdentity).forEach {
            val request = Request.Builder()
                    .url("http://${it.listenAddress}/api/builder/buildings")
                    .build()
            val response = OkHttpClient().newCall(request).execute()
            assertEquals("[ ]", response.body().string())

            val address = "test address"
            val price = 1

            val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{\"address\": \"$address\", \"price\": $price}")
            val request2 = Request.Builder()
                    .url("http://${it.listenAddress}/api/builder/building/new")
                    .post(body)
                    .build()
            val response2 = OkHttpClient().newCall(request2).execute()
            assertEquals(HttpStatus.CREATED_201, response2.code(), response2.body().string())

            val request3 = Request.Builder()
                    .url("http://${it.listenAddress}/api/builder/buildings")
                    .build()
            val response3 = OkHttpClient().newCall(request3).execute()

            val parser = JSONParser()
            val jsonArray = parser.parse(response3.body().string()) as JSONArray
            val jsonObj = jsonArray[0] as JSONObject
            assertEquals(address, jsonObj.get("address"))
            assertEquals(price, jsonObj.get("price").toString().toInt())
        }
    }

/*    init {
/*        listOf(builder).forEach {
            it.registerInitiatedFlow(Responder::class.java)
        }*/

        val builderUser = User("builder1", "test", permissions = setOf("ALL"))
        driver(DriverParameters(isDebug = true, waitForAllNodesToFinish = true)) {
            val (builder) = listOf(
                    startNode(providedName = CordaX500Name("Builder", "London", "GB"), rpcUsers = listOf(builderUser ))).map { it.getOrThrow() }

            startWebserver(builder)
        }
    }
*//*
    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()
*/
    /*
    @Test
    fun `dummy test`() {

    }*/
}