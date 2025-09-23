package redis.embedded;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.embedded.model.OsArchitecture;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.embedded.RedisServer.SERVER_READY_PATTERN;
import static redis.embedded.RedisServer.newRedisServer;
import static redis.embedded.core.ExecutableProvider.newJarResourceProvider;
import static redis.embedded.model.OsArchitecture.MAC_OS_X_ARM64;
import static redis.embedded.model.OsArchitecture.MAC_OS_X_X86_64;
import static redis.embedded.model.OsArchitecture.UNIX_ARM64;
import static redis.embedded.model.OsArchitecture.UNIX_X86_64;
import static redis.embedded.model.OsArchitecture.WINDOWS_X86_64;

class RedisServerTest {

    private RedisServer redisServer;

    @AfterEach
    void stopRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }

    @Test
    void testSimpleRun() throws Exception {
        redisServer = new RedisServer(6381);
        redisServer.start();
    }

    @Test
    void shouldAllowMultipleRunsWithoutStop() throws IOException {
        redisServer = new RedisServer(6381);
        redisServer.start();
        redisServer.start();
    }

    @Test
    void shouldAllowSubsequentRuns() throws IOException {
        redisServer = new RedisServer(6381);
        redisServer.start();
        redisServer.stop();

        redisServer.start();
        redisServer.stop();

        redisServer.start();
        redisServer.stop();
    }

    @Test
    void testSimpleOperationsAfterRun() throws IOException {
        redisServer = new RedisServer(6381);
        redisServer.start();

        try (final JedisPool pool = new JedisPool("localhost", 6381);
             final Jedis jedis = pool.getResource()) {
            jedis.mset("abc", "1", "def", "2");

            assertEquals("1", jedis.mget("abc").get(0));
            assertEquals("2", jedis.mget("def").get(0));
            assertNull(jedis.mget("xyz").get(0));
        }
    }

    @Test
    void shouldIndicateInactiveBeforeStart() throws IOException {
        redisServer = new RedisServer(6381);
        assertFalse(redisServer.isActive());
    }

    @Test
    void shouldIndicateActiveAfterStart() throws IOException {
        redisServer = new RedisServer(6381);
        redisServer.start();
        assertTrue(redisServer.isActive());
    }

    @Test
    void shouldIndicateInactiveAfterStop() throws IOException {
        redisServer = new RedisServer(6381);
        redisServer.start();
        redisServer.stop();
        assertFalse(redisServer.isActive());
    }

    @Test
    void shouldOverrideDefaultExecutable() throws IOException {
        final Map<OsArchitecture, String> map = new HashMap<>();
        map.put(UNIX_X86_64, "redis-server-6.2.6-v5-linux-amd64");
        map.put(UNIX_ARM64, "redis-server-6.2.7-linux-arm64");
        map.put(WINDOWS_X86_64, "redis-server-5.0.14.1-windows-amd64.exe");
        map.put(MAC_OS_X_X86_64, "redis-server-6.2.6-v5-darwin-amd64");
        map.put(MAC_OS_X_ARM64, "redis-server-6.2.6-v5-darwin-arm64");

        redisServer = newRedisServer()
                .executableProvider(newJarResourceProvider(map))
                .build();
    }

    @Test
    void shouldFailWhenBadExecutableGiven() throws IOException {
        final Map<OsArchitecture, String> buggyMap = new HashMap<>();
        buggyMap.put(UNIX_X86_64, "some");
        buggyMap.put(UNIX_ARM64, "some");
        buggyMap.put(WINDOWS_X86_64, "some");
        buggyMap.put(MAC_OS_X_X86_64, "some");
        buggyMap.put(MAC_OS_X_ARM64, "some");

        assertThatThrownBy(() -> redisServer = newRedisServer()
                .executableProvider(newJarResourceProvider(buggyMap))
                .build()).isExactlyInstanceOf(FileNotFoundException.class);
    }

    @Test
    void testAwaitRedisServerReady() throws IOException {
        testReadyPattern("/redis-2.x-standalone-startup-output.txt", SERVER_READY_PATTERN);
        testReadyPattern("/redis-3.x-standalone-startup-output.txt", SERVER_READY_PATTERN);
        testReadyPattern("/redis-4.x-standalone-startup-output.txt", SERVER_READY_PATTERN);
    }

    private static void testReadyPattern(final String resourcePath, final Pattern readyPattern) throws IOException {
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
