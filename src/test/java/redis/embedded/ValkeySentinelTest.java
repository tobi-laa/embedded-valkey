package redis.embedded;

import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel;
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static redis.embedded.util.Collections.newHashSet;

class ValkeySentinelTest {
    private final String bindAddress = "localhost";

    private ValkeySentinel sentinel;
    private ValkeyStandalone server;

    @AfterEach
    void stopSentinel() throws IOException {
        try {
            if (sentinel != null && sentinel.getActive()) {
                sentinel.stop();
            }
        } finally {
            if (server != null && server.getActive()) {
                server.stop();
            }
        }
    }

    @Test
    void testSimpleRun() throws InterruptedException, IOException {
        server = ValkeyStandalone.builder().build();
        sentinel = ValkeySentinel.builder().bind(bindAddress).build();
        sentinel.start();
        server.start();
        TimeUnit.SECONDS.sleep(1);
        server.stop();
        sentinel.stop();
    }

    @Test
    void shouldAllowSubsequentRuns() throws IOException {
        sentinel = ValkeySentinel.builder().bind(bindAddress).build();
        sentinel.start();
        sentinel.stop();

        sentinel.start();
        sentinel.stop();

        sentinel.start();
        sentinel.stop();
    }

    @Test
    void testSimpleOperationsAfterRun() throws IOException {
        server = ValkeyStandalone.builder().build();
        sentinel = ValkeySentinel.builder().bind(bindAddress).build();
        server.start();
        sentinel.start();

        JedisSentinelPool pool = null;
        Jedis jedis = null;
        try {
            pool = new JedisSentinelPool("mymain", newHashSet("localhost:26379"));
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
            sentinel.stop();
            server.stop();
        }
    }

}
