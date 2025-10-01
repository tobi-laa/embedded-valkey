package redis.embedded;

import io.github.tobi.laa.embedded.valkey.cluster.highavailability.ValkeyHighAvailability;
import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel;
import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinelBuilder;
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.embedded.util.JedisUtil;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static redis.embedded.util.Collections.newHashSet;

public class ValkeyHighAvailibilityTest {
    private final ValkeySentinelBuilder sentinelBuilder = ValkeySentinel.builder();
    private String bindAddress;

    private ValkeySentinel sentinel1;
    private ValkeySentinel sentinel2;
    private ValkeyStandalone master1;
    private ValkeyStandalone master2;

    private ValkeyHighAvailability instance;

    @Before
    public void setUp() throws UnknownHostException {
        sentinel1 = mock(ValkeySentinel.class);
        sentinel2 = mock(ValkeySentinel.class);
        master1 = mock(ValkeyStandalone.class);
        master2 = mock(ValkeyStandalone.class);
    }

    @Test
    public void testSimpleOperationsAfterRunWithSingleMasterNoSlavesCluster() throws IOException {
        final ValkeyHighAvailability cluster = ValkeyHighAvailability.builder()
                .withSentinelBuilder(sentinelBuilder)
                .sentinelCount(1)
                .replicationGroup("ourmaster", 0)
                .build();
        cluster.start();

        JedisSentinelPool pool = null;
        Jedis jedis = null;
        try {
            pool = new JedisSentinelPool("ourmaster", newHashSet("localhost:26379"));
            jedis = testPool(pool);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            if (pool != null) {
                pool.destroy();
            }
            cluster.stop();
        }
    }

    @Test
    public void testSimpleOperationsAfterRunWithSingleMasterAndOneSlave() throws IOException {
        final ValkeyHighAvailability cluster = ValkeyHighAvailability.builder()
                .withSentinelBuilder(sentinelBuilder)
                .sentinelCount(1)
                .replicationGroup("ourmaster", 1)
                .build();
        cluster.start();

        JedisSentinelPool pool = null;
        Jedis jedis = null;
        try {
            pool = new JedisSentinelPool("ourmaster", newHashSet("localhost:26379"));
            jedis = testPool(pool);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            if (pool != null) {
                pool.destroy();
            }
            cluster.stop();
        }
    }

    @Test
    public void testSimpleOperationsAfterRunWithSingleMasterMultipleSlaves() throws IOException {
        final ValkeyHighAvailability cluster = ValkeyHighAvailability.builder()
                .withSentinelBuilder(sentinelBuilder)
                .sentinelCount(1)
                .replicationGroup("ourmaster", 2)
                .build();
        cluster.start();

        JedisSentinelPool pool = null;
        Jedis jedis = null;
        try {
            pool = new JedisSentinelPool("ourmaster", newHashSet("localhost:26379"));
            jedis = testPool(pool);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            if (pool != null) {
                pool.destroy();
            }
            cluster.stop();
        }
    }

    @Test
    public void testSimpleOperationsAfterRunWithTwoSentinelsSingleMasterMultipleSlaves() throws IOException {
        final ValkeyHighAvailability cluster = ValkeyHighAvailability.builder()
                .withSentinelBuilder(sentinelBuilder)
                .sentinelCount(2)
                .replicationGroup("ourmaster", 2)
                .build();
        cluster.start();

        JedisSentinelPool pool = null;
        Jedis jedis = null;
        try {
            pool = new JedisSentinelPool("ourmaster", newHashSet("localhost:26379", "localhost:26380"));
            jedis = testPool(pool);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            if (pool != null) {
                pool.destroy();
            }
            cluster.stop();
        }
    }

    @Test
    public void testSimpleOperationsAfterRunWithTwoPredefinedSentinelsSingleMasterMultipleSlaves() throws IOException {
        final List<Integer> sentinelPorts = Arrays.asList(26381, 26382);
        final ValkeyHighAvailability cluster = ValkeyHighAvailability.builder()
                .withSentinelBuilder(sentinelBuilder)
                .sentinelPorts(sentinelPorts)
                .replicationGroup("ourmaster", 2)
                .build();
        cluster.start();
        final Set<String> sentinelHosts = JedisUtil.portsToJedisHosts(sentinelPorts);

        JedisSentinelPool pool = null;
        Jedis jedis = null;
        try {
            pool = new JedisSentinelPool("ourmaster", sentinelHosts);
            jedis = testPool(pool);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            if (pool != null) {
                pool.destroy();
            }
            cluster.stop();
        }
    }

    @Test
    public void testSimpleOperationsAfterRunWithThreeSentinelsThreeMastersOneSlavePerMasterCluster() throws IOException {
        final String master1 = "master1";
        final String master2 = "master2";
        final String master3 = "master3";
        final ValkeyHighAvailability cluster = ValkeyHighAvailability.builder()
                .withSentinelBuilder(sentinelBuilder)
                .sentinelCount(3)
                .quorumSize(2)
                .replicationGroup(master1, 1)
                .replicationGroup(master2, 1)
                .replicationGroup(master3, 1)
                .build();
        cluster.start();

        JedisSentinelPool pool1 = null;
        JedisSentinelPool pool2 = null;
        JedisSentinelPool pool3 = null;
        Jedis jedis1 = null;
        Jedis jedis2 = null;
        Jedis jedis3 = null;
        try {
            pool1 = new JedisSentinelPool(master1, newHashSet("localhost:26379", "localhost:26380", "localhost:26381"));
            pool2 = new JedisSentinelPool(master2, newHashSet("localhost:26379", "localhost:26380", "localhost:26381"));
            pool3 = new JedisSentinelPool(master3, newHashSet("localhost:26379", "localhost:26380", "localhost:26381"));
            jedis1 = testPool(pool1);
            jedis2 = testPool(pool2);
            jedis3 = testPool(pool3);
        } finally {
            if (jedis1 != null) {
                jedis1.close();
            }
            if (pool1 != null) {
                pool1.destroy();
            }
            if (jedis2 != null) {
                jedis2.close();
            }
            if (pool2 != null) {
                pool2.destroy();
            }
            if (jedis3 != null) {
                jedis3.close();
            }
            if (pool3 != null) {
                pool3.destroy();
            }
            cluster.stop();
        }
    }

    @Test
    public void testSimpleOperationsAfterRunWithThreeSentinelsThreeMastersOneSlavePerMasterEphemeralCluster() throws IOException {
        final String master1 = "master1";
        final String master2 = "master2";
        final String master3 = "master3";
        final ValkeyHighAvailability cluster = ValkeyHighAvailability.builder().withSentinelBuilder(sentinelBuilder)
                .ephemeral().sentinelCount(3).quorumSize(2)
                .replicationGroup(master1, 1)
                .replicationGroup(master2, 1)
                .replicationGroup(master3, 1)
                .build();
        cluster.start();
        final Set<String> sentinelHosts = JedisUtil.sentinelHosts(cluster);

        JedisSentinelPool pool1 = null;
        JedisSentinelPool pool2 = null;
        JedisSentinelPool pool3 = null;
        Jedis jedis1 = null;
        Jedis jedis2 = null;
        Jedis jedis3 = null;
        try {
            pool1 = new JedisSentinelPool(master1, sentinelHosts);
            pool2 = new JedisSentinelPool(master2, sentinelHosts);
            pool3 = new JedisSentinelPool(master3, sentinelHosts);
            jedis1 = testPool(pool1);
            jedis2 = testPool(pool2);
            jedis3 = testPool(pool3);
        } finally {
            if (jedis1 != null) {
                jedis1.close();
            }
            if (pool1 != null) {
                pool1.destroy();
            }
            if (jedis2 != null) {
                jedis2.close();
            }
            if (pool2 != null) {
                pool2.destroy();
            }
            if (jedis3 != null) {
                jedis3.close();
            }
            if (pool3 != null) {
                pool3.destroy();
            }
            cluster.stop();
        }
    }

    private Jedis testPool(final JedisSentinelPool pool) {
        Jedis jedis;
        jedis = pool.getResource();
        jedis.mset("abc", "1", "def", "2");

        assertEquals("1", jedis.mget("abc").get(0));
        assertEquals("2", jedis.mget("def").get(0));
        assertNull(jedis.mget("xyz").get(0));
        return jedis;
    }

}
