package com.redislabs.university.examples;

import com.redislabs.university.HostPort;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class StreamsTest {
    private Jedis jedis;
    private String streamKey;
    private Map<String, String> entry1;
    private Map<String, String> entry2;


    @Before
    public void setUp() {
        jedis = new Jedis(HostPort.getRedisHost(), HostPort.getRedisPort());
        if (!HostPort.getRedisPassword().isEmpty()) {
            jedis.auth(HostPort.getRedisPassword());
        }
        streamKey = "test:stream";
        entry1 = new HashMap<>();
        entry1.put("siteId", "1");
        entry2 = new HashMap<>();
        entry2.put("siteId", "2");
    }

    @After
    public void tearDown() {
        jedis.del(streamKey);
        jedis.close();
    }

    @Test
    public void testStream() {
        final long numberOfSolarSites = 300L;
        final long measurementsPerHour = 60L;
        final long hoursPerDay = 24L;
        final long maxDays = 14L;
        final long maxStreamEntries =
                numberOfSolarSites * measurementsPerHour * hoursPerDay * maxDays;

        final Map<String, String> entry = new HashMap<>();
        entry.put("siteId", "1");
        entry.put("tempC", "18.0");

        final StreamEntryID id = jedis.xadd(streamKey, StreamEntryID.NEW_ENTRY, entry,
                maxStreamEntries, true);
        final List<StreamEntry> results = jedis.xrevrange(streamKey, null, null, 1);

        assertThat(results.size(), is(1));
        assertThat(results.get(0).getID(), is(id));
        assertThat(results.get(0).getFields(), is(entry));
    }

    @Test
    public void testStreamWithPipeline() {
        final Pipeline p = jedis.pipelined();
        final Response<StreamEntryID> id1 = p.xadd(streamKey, StreamEntryID.NEW_ENTRY, entry1);
        final Response<StreamEntryID> id2 = p.xadd(streamKey, StreamEntryID.NEW_ENTRY, entry2);
        final Response<List<StreamEntry>> results = p.xrange(streamKey, null, null, 2);
        p.sync();

        final List<StreamEntry> entries = results.get();
        assertThat(entries.size(), is(2));
        assertThat(entries.get(0).getID(), is(id1.get()));
        assertThat(entries.get(1).getID(), is(id2.get()));
    }

    @Test
    public void testStreamWithTransaction() {
        final Transaction t = jedis.multi();
        t.xadd(streamKey, StreamEntryID.NEW_ENTRY, entry1);
        t.xadd(streamKey, StreamEntryID.NEW_ENTRY, entry2);
        final Response<List<StreamEntry>> results = t.xrange(streamKey, null, null, 2);
        t.exec();
        assertThat(results.get().size(), is(2));
    }
}
