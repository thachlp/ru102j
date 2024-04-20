package com.redislabs.university.resources;

import com.google.common.net.HttpHeaders;
import com.redislabs.university.dao.CapacityDao;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/capacity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CapacityResource {

    private final CapacityDao capacityDao;

    public CapacityResource(CapacityDao capacityDao) {
        this.capacityDao = capacityDao;
    }

    @GET
    public Response getCapacity(@QueryParam("limit")
                                    @DefaultValue("10")
                                    Integer limit) {
        return Response.ok(capacityDao.getReport(limit))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .build();
    }
}
