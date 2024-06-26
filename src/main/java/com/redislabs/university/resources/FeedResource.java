package com.redislabs.university.resources;

import com.redislabs.university.api.MeterReading;
import com.redislabs.university.dao.FeedDao;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/capacity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FeedResource {

    private final FeedDao feedDao;
    private static final Integer FEED_DEFAULT_LIMIT = 20;

    public FeedResource(FeedDao feedDao) {
        this.feedDao = feedDao;
    }

    @GET
    public Response getAllEntries(@QueryParam("limit") Integer limit) {
        if (limit == null) {
            limit = FEED_DEFAULT_LIMIT;
        }
        final List<MeterReading> readings = feedDao.getRecentGlobal(limit);
        return Response.ok(readings)
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getSingleFeed(@PathParam("id") Long siteId,
                                            @QueryParam("limit") Integer limit) {
        if (limit == null) {
            limit = FEED_DEFAULT_LIMIT;
        }
        final List<MeterReading> readings = feedDao.getRecentForSite(siteId, limit);
        return Response.ok(readings)
                .build();
    }
}
