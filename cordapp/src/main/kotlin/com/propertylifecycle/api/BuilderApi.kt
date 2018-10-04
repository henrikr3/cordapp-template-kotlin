package com.propertylifecycle.api

import net.corda.core.messaging.CordaRPCOps
import org.eclipse.jetty.http.HttpStatus
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


class ApiResult(var status: String, var info: String) {}
class Building(var id: Int, val address: String, val price: Int) {}

@Path("builder")
class BuilderApi(val rpcOps: CordaRPCOps) {
    private val buildings = mutableListOf<Building>()

    @POST
    @Path("building/new")
    @Produces(MediaType.APPLICATION_JSON)
    fun buildingNew(building: Building): Response {
        if(building.address.isEmpty())
            return Response.status(HttpStatus.BAD_REQUEST_400).entity("Address is a mandatory field.").build()
        if(building.price <= 0)
            return Response.status(HttpStatus.BAD_REQUEST_400).entity("Price should be greater than zero.").build()

        val maxId = buildings.maxBy { it ->  it.id }
        building.id = (maxId?.id  ?: 0) + 1
        if(true) {
            buildings.add(building)
            return Response.status(HttpStatus.CREATED_201).entity(ApiResult("success", "NEW BUILDING COMPLETED")).build()
        } else {
            return Response.status(HttpStatus.BAD_REQUEST_400).entity(ApiResult("error", "FAILED TO CREATE BUILDING")).build()
        }
    }

    @POST
    @Path("building/{id}/sell")
    @Produces(MediaType.APPLICATION_JSON)
    fun buildingSell(@PathParam("id") id: Int): Response {
        val building = buildings[id]
        if(true) {
            return Response.status(HttpStatus.CREATED_201).entity(ApiResult("success", "SELL BUILDING:{$building.id}")).build()
        } else {
            return Response.status(HttpStatus.BAD_REQUEST_400).entity(ApiResult("error", "FAILED TO SELL BUILDING:{$building.id}")).build()
        }
    }

    @GET
    @Path("buildings")
    @Produces(MediaType.APPLICATION_JSON)
    fun buildings(): Response {
        return Response.ok(buildings).build()
    }
}
