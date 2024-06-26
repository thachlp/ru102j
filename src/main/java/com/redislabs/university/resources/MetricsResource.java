package com.redislabs.university.resources;

import com.google.common.net.HttpHeaders;
import com.redislabs.university.api.Measurement;
import com.redislabs.university.api.Plot;
import com.redislabs.university.api.MetricUnit;
import com.redislabs.university.dao.MetricDao;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetricsResource {

    private static final Integer DEFAULT_METRIC_COUNT = 120;

    private final MetricDao metricDao;

    public MetricsResource(MetricDao dayMetricDao) {
        metricDao = dayMetricDao;
    }

    @GET
    @Path("/{siteId}")
    public Response getSiteMetrics(@PathParam("siteId") Long siteId,
                                   @QueryParam("count") Integer count) {
        final List<Plot> plots = new ArrayList<>();
        if (count == null) {
            count = DEFAULT_METRIC_COUNT;
        }
        // Get kWhGenerated measurements
        final List<Measurement> generated = metricDao.getRecent(siteId, MetricUnit.WH_GENERATED,
                ZonedDateTime.now(ZoneOffset.UTC), count);
        plots.add(new Plot("Watt-Hours Generated", generated));

        // Get kWhUsed measurements
        final List<Measurement> used = metricDao.getRecent(siteId, MetricUnit.WH_USED,
                ZonedDateTime.now(ZoneOffset.UTC), count);
        plots.add(new Plot("Watt-Hours Used", used));

        return Response.ok(plots)
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .build();
    }
}
