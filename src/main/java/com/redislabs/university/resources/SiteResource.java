package com.redislabs.university.resources;

import com.google.common.net.HttpHeaders;
import com.redislabs.university.api.Site;
import com.redislabs.university.dao.SiteDao;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/sites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SiteResource {

    private final SiteDao siteDao;

    public SiteResource(SiteDao siteDao) {
        this.siteDao = siteDao;
    }

    @GET
    public Response getSites() {
        return Response.ok(siteDao.findAll())
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getSite(@PathParam("id") Long id) {
        Site site = siteDao.findById(id);
        if (site == null) {
            return Response.noContent().status(404).build();
        }
        return Response.ok(site).header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*").build();
    }
}
