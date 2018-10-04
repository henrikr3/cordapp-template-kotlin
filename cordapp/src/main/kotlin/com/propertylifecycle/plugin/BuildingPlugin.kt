package com.propertylifecycle.plugin

import com.propertylifecycle.api.BuilderApi
import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

// ***********
// * Plugins *
// ***********
class TemplateWebPlugin : WebServerPluginRegistry {
    // A list of lambdas that create objects exposing web JAX-RS REST APIs.
    override val webApis: List<java.util.function.Function<CordaRPCOps, out Any>> = listOf(Function(::BuilderApi))
    //A list of directories in the resources directory that will be served by Jetty under /web.
    // This propertylifecycle's web frontend is accessible at /web/propertylifecycle.
    override val staticServeDirs: Map<String, String> = mapOf(
            // This will serve the webBuilder directory in resources to /web/builder
            "builder" to javaClass.classLoader.getResource("webBuilder").toExternalForm()
    )
}
