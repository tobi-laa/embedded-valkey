package redis.embedded;

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;

import static io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone.builder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RedisServerClusterTest {

    private ValkeyStandalone valkeyStandalone1;
    private ValkeyStandalone valkeyStandalone2;

    @Before
    public void setUp() throws IOException {
        valkeyStandalone1 = builder().port(6300).build();
        valkeyStandalone2 = builder().port(6301)
                .replicaOf("localhost", 6300)
                .build();

        valkeyStandalone1.start();
        valkeyStandalone2.start();
    }

    @Test
    public void testSimpleOperationsAfterRun() {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            pool = new JedisPool("localhost", 6300);
            jedis = pool.getResource();
            jedis.mset("abc", "1", "def", "2");

            assertEquals("1", jedis.mget("abc").get(0));
            assertEquals("2", jedis.mget("def").get(0));
            assertNull(jedis.mget("xyz").get(0));
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            if (pool != null) {
                pool.destroy();
            }
        }
    }


    @After
    public void tearDown() throws IOException {
        valkeyStandalone1.stop();
        valkeyStandalone2.stop();
    }

}
