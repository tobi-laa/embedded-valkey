package redis.embedded;

import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static redis.embedded.RedisSentinel.SENTINEL_READY_PATTERN;
import static redis.embedded.util.Collections.newHashSet;

public class RedisSentinelTest {
    private final String bindAddress = "localhost";

    private RedisSentinel sentinel;
    private RedisServer server;

    @Test(timeout = 3000L)
    public void testSimpleRun() throws InterruptedException, IOException {
        server = new RedisServer();
        sentinel = RedisSentinel.newRedisSentinel().bind(bindAddress).build();
        sentinel.start();
        server.start();
        TimeUnit.SECONDS.sleep(1);
        server.stop();
        sentinel.stop();
    }

    @Test
    public void shouldAllowSubsequentRuns() throws IOException {
        sentinel = RedisSentinel.newRedisSentinel().bind(bindAddress).build();
        sentinel.start();
        sentinel.stop();

        sentinel.start();
        sentinel.stop();

        sentinel.start();
        sentinel.stop();
    }

    @Test
    public void testSimpleOperationsAfterRun() throws IOException {
        server = new RedisServer();
        sentinel = RedisSentinel.newRedisSentinel().bind(bindAddress).build();
        server.start();
        sentinel.start();

        JedisSentinelPool pool = null;
        Jedis jedis = null;
        try {
            pool = new JedisSentinelPool("mymaster", newHashSet("localhost:26379"));
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

    @Test
    public void testAwaitRedisSentinelReady() throws Exception {
        assertReadyPattern("/redis-2.x-sentinel-startup-output.txt", SENTINEL_READY_PATTERN);
        assertReadyPattern("/redis-3.x-sentinel-startup-output.txt", SENTINEL_READY_PATTERN);
        assertReadyPattern("/redis-4.x-sentinel-startup-output.txt", SENTINEL_READY_PATTERN);
    }

    private static void assertReadyPattern(final String resourcePath, final Pattern readyPattern) throws IOException {
        final InputStream in = RedisServerTest.class.getResourceAsStream(resourcePath);
        assertNotNull(in);
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            do {
                line = reader.readLine();
                assertNotNull(line);
            } while (!readyPattern.matcher(line).matches());
        }
    }

}
