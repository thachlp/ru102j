package com.redislabs.university.dao;

import com.redislabs.university.HostPort;
import com.redislabs.university.TestKeyManager;
import com.redislabs.university.api.Coordinate;
import com.redislabs.university.api.GeoQuery;
import com.redislabs.university.api.MeterReading;
import com.redislabs.university.api.Site;
import org.junit.*;
import org.junit.rules.ExpectedException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.*;

public class SiteGeoDaoRedisImplTest {

    private static JedisPool jedisPool;
    private static Jedis jedis;
    private static TestKeyManager keyManager;
    private Set<Site> sites;

    @BeforeClass
    public static void setUp() throws Exception {
        final String password = HostPort.getRedisPassword();
        if (!password.isEmpty()) {
            jedisPool = new JedisPool(new JedisPoolConfig(), HostPort.getRedisHost(), HostPort.getRedisPort(), 2000, password);
        } else {
            jedisPool = new JedisPool(HostPort.getRedisHost(), HostPort.getRedisPort());
        }
        jedis = new Jedis(HostPort.getRedisHost(), HostPort.getRedisPort());
        if (!password.isEmpty()) {
            jedis.auth(password);
        }
        keyManager = new TestKeyManager("test");
    }

    @AfterClass
    public static void tearDown() {
        jedisPool.destroy();
        jedis.close();
    }

    @After
    public void flush() {
        keyManager.deleteKeys(jedis);
    }

    @Before
    public void generateData() {
        sites = new HashSet<>();

        final Site site1 = new Site(1, 4.5, 3, "637 Britannia Drive",
                "Vallejo", "CA", "94591");
        site1.setCoordinate(new Coordinate("-122.193849", "38.10476999999999"));
        sites.add(site1);

        final Site site2 = new Site(2, 4.5, 3, "31353 Santa Elena Way",
                "Union City", "CA", "94587");
        site2.setCoordinate(new Coordinate("-122.059762", "37.593981"));
        sites.add(site2);

        final Site site3 = new Site(3, 4.5, 3, "1732 27th Avenue",
                "Oakland", "CA", "94601");
        site3.setCoordinate(new Coordinate("-122.228238", "37.783431"));
        sites.add(site3);
    }

    @Test
    public void findAllWithMultipleSites() {
        final SiteGeoDao dao = new SiteGeoDaoRedisImpl(jedisPool);
        // Insert all sites
        for (Site site : sites) {
            dao.insert(site);
        }

        assertEquals(dao.findAll(), sites);
    }

    @Test
    public void findAllWithEmptySites() {
        final SiteDaoRedisImpl dao = new SiteDaoRedisImpl(jedisPool);
        assertEquals(dao.findAll(), empty());
    }

    @Test
    public void findByGeo() {
        // Insert sites
        final SiteGeoDao dao = new SiteGeoDaoRedisImpl(jedisPool);
        for (Site site : sites) {
            dao.insert(site);
        }

        final Coordinate oakland = new Coordinate("-122.272476", "37.804829");
        final Set<Site> oaklandSites = dao.findByGeo(new GeoQuery(oakland, 10.0, "KM"));
        assertEquals(1, oaklandSites.size());

        final Coordinate vallejo = new Coordinate("-122.256637", "38.104086");
        final Set<Site> vallejoSites = dao.findByGeo(new GeoQuery(vallejo, 10.0, "KM"));
        assertEquals(1, vallejoSites.size());


        final Coordinate unionCity = new Coordinate("-122.081630", "37.596323");
        final Set<Site> unionCitySites = dao.findByGeo(new GeoQuery(unionCity, 10.0, "KM"));
        assertEquals(1, unionCitySites.size());

        // Expand the radius to return all 3 sites
        final Set<Site> californiaSites = dao.findByGeo(new GeoQuery(unionCity, 60.0, "KM"));
        assertEquals(1, californiaSites.size());
    }

    // Challenge #5
    @Test
    public void findByGeoWithExcessCapacity() {
        final SiteGeoDao siteDao = new SiteGeoDaoRedisImpl(jedisPool);
        final CapacityDao capacityDao = new CapacityDaoRedisImpl(jedisPool);
        final Site vallejo = new Site(1, 4.5, 3, "637 Britannia Drive",
                "Vallejo", "CA", "94591");
        final Coordinate vallejoCoord = new Coordinate("-122.256637", "38.104086");
        vallejo.setCoordinate(vallejoCoord);
        siteDao.insert(vallejo);

        // This site is returned when we're not looking for excess capacity.
        Set<Site> sites = siteDao.findByGeo(new GeoQuery(vallejoCoord, 10.0, "KM"));
        assertEquals(1, sites.size());
        assertTrue(sites.contains(vallejo));

        // Simulate changing a meter reading with no excess capacity
        final MeterReading reading = new MeterReading();
        reading.setSiteId(vallejo.getId());
        reading.setWhUsed(1.0);
        reading.setWhGenerated(0.0);
        capacityDao.update(reading);

        // In this case, no sites are returned on the excess capacity query
        sites = siteDao.findByGeo(new GeoQuery(vallejoCoord, 10.0, "KM", true));
        assertEquals(0, sites.size());

        // Simulate changing a meter reading indicating excess capacity
        reading.setWhGenerated(2.0);
        capacityDao.update(reading);

        // In this case, one site is returned on the excess capacity query
        sites = siteDao.findByGeo(new GeoQuery(vallejoCoord, 10.0, "KM", true));
        assertEquals(1, sites.size());
        assertTrue(sites.contains(vallejo));
    }

    @Test
    public void insert() {
        final SiteGeoDao dao = new SiteGeoDaoRedisImpl(jedisPool);
        final Site vallejo = new Site(7, 4.5, 3, "637 Britannia Drive",
                "Vallejo", "CA", "94591");
        vallejo.setCoordinate(new Coordinate("-122.193849", "38.10476999999999"));
        dao.insert(vallejo);
        final String key = RedisSchema.getSiteHashKey(vallejo.getId());
        final Map<String, String> response = jedis.hgetAll(key);
        assertEquals(vallejo.getPanels().toString(), response.get("panels"));
        assertEquals(vallejo.getCapacity().toString(), response.get("capacity"));
        assertEquals(vallejo.getAddress(), response.get("address"));
        assertEquals(vallejo.getCity(), response.get("city"));
        assertEquals(vallejo.getState(), response.get("state"));
        assertEquals(vallejo.getPostalCode(), response.get("postalCode"));
        assertEquals(vallejo.getCoordinate().getLat().toString(), response.get("lat"));
        assertEquals(vallejo.getCoordinate().getLng().toString(), response.get("lng"));
    }

    @Test
    public void insertFailsWithoutCoordinate() {
        final SiteGeoDao dao = new SiteGeoDaoRedisImpl(jedisPool);
        final Site vallejo = new Site(7, 4.5, 3, "637 Britannia Drive",
                "Vallejo", "CA", "94591");
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            dao.insert(vallejo);
        });
        assertEquals("Coordinate required for Geo insert.", thrown.getMessage());
    }
}