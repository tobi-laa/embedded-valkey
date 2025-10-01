package redis.embedded;

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import static io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone.SERVER_READY_PATTERN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValkeyStandaloneTest {

    private ValkeyStandalone valkeyStandalone;

    @AfterEach
    void stopRedis() throws IOException {
        if (valkeyStandalone != null && valkeyStandalone.active()) {
            valkeyStandalone.stop();
        }
    }

    @Test
    void testSimpleRun() throws Exception {
        valkeyStandalone = ValkeyStandalone.builder().port(6381).build();
        valkeyStandalone.start();
    }

    @Test
    void shouldAllowMultipleRunsWithoutStop() throws IOException {
        valkeyStandalone = ValkeyStandalone.builder().port(6381).build();
        valkeyStandalone.start();
        valkeyStandalone.start();
    }

    @Test
    void shouldAllowSubsequentRuns() throws IOException {
        valkeyStandalone = ValkeyStandalone.builder().port(6381).build();
        valkeyStandalone.start();
        valkeyStandalone.stop();

        valkeyStandalone.start();
        valkeyStandalone.stop();

        valkeyStandalone.start();
        valkeyStandalone.stop();
    }

    @Test
    void testSimpleOperationsAfterRun() throws IOException {
        valkeyStandalone = ValkeyStandalone.builder().port(6381).build();
        valkeyStandalone.start();

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
        valkeyStandalone = ValkeyStandalone.builder().port(6381).build();
        assertFalse(valkeyStandalone.active());
    }

    @Test
    void shouldIndicateActiveAfterStart() throws IOException {
        valkeyStandalone = ValkeyStandalone.builder().port(6381).build();
        valkeyStandalone.start();
        assertTrue(valkeyStandalone.active());
    }

    @Test
    void shouldIndicateInactiveAfterStop() throws IOException {
        valkeyStandalone = ValkeyStandalone.builder().port(6381).build();
        valkeyStandalone.start();
        valkeyStandalone.stop();
        assertFalse(valkeyStandalone.active());
    }

//    @Disabled
//    @Test
//    void shouldOverrideDefaultExecutable() throws IOException {
//        final Map<OsArchitecture, String> map = new HashMap<>();
//        map.put(UNIX_X86_64, "redis-server-6.2.6-v5-linux-amd64");
//        map.put(UNIX_ARM64, "redis-server-6.2.7-linux-arm64");
//        map.put(WINDOWS_X86_64, "redis-server-5.0.14.1-windows-amd64.exe");
//        map.put(MAC_OS_X_X86_64, "redis-server-6.2.6-v5-darwin-amd64");
//        map.put(MAC_OS_X_ARM64, "redis-server-6.2.6-v5-darwin-arm64");
//
//        redisServer = newRedisServer()
//                .executableProvider(newJarResourceProvider(map))
//                .build();
//    }
//
//    @Test
//    void shouldFailWhenBadExecutableGiven() throws IOException {
//        final Map<OsArchitecture, String> buggyMap = new HashMap<>();
//        buggyMap.put(UNIX_X86_64, "some");
//        buggyMap.put(UNIX_ARM64, "some");
//        buggyMap.put(WINDOWS_X86_64, "some");
//        buggyMap.put(MAC_OS_X_X86_64, "some");
//        buggyMap.put(MAC_OS_X_ARM64, "some");
//
//        assertThatThrownBy(() -> redisServer = newRedisServer()
//                .executableProvider(newJarResourceProvider(buggyMap))
//                .build()).isExactlyInstanceOf(FileNotFoundException.class);
//    }

    @Test
    void testAwaitRedisServerReady() throws IOException {
        testReadyPattern("/redis-2.x-standalone-startup-output.txt", SERVER_READY_PATTERN);
        testReadyPattern("/redis-3.x-standalone-startup-output.txt", SERVER_READY_PATTERN);
        testReadyPattern("/redis-4.x-standalone-startup-output.txt", SERVER_READY_PATTERN);
    }

    private static void testReadyPattern(final String resourcePath, final Pattern readyPattern) throws IOException {
        final InputStream in = ValkeyStandaloneTest.class.getResourceAsStream(resourcePath);
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
