package io.github.tobi.laa.embedded.valkey.standalone

import io.github.tobi.laa.embedded.valkey.IntegrationTest
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone.Companion.builder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import redis.clients.jedis.JedisPool
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.regex.Pattern

@IntegrationTest
internal class ValkeyStandaloneTest {
    private var valkeyStandalone: ValkeyStandalone? = null

    @Test
    @Throws(Exception::class)
    fun testSimpleRun() {
        valkeyStandalone = builder().port(6381).build()
        valkeyStandalone!!.start()
    }

    @Test
    @Throws(IOException::class)
    fun shouldAllowMultipleRunsWithoutStop() {
        valkeyStandalone = builder().port(6381).build()
        valkeyStandalone!!.start()
        valkeyStandalone!!.start()
    }

    @Test
    @Throws(IOException::class)
    fun shouldAllowSubsequentRuns() {
        valkeyStandalone = builder().port(6381).build()
        valkeyStandalone!!.start()
        valkeyStandalone!!.stop()

        valkeyStandalone!!.start()
        valkeyStandalone!!.stop()

        valkeyStandalone!!.start()
        valkeyStandalone!!.stop()
    }

    @Test
    @Throws(IOException::class)
    fun testSimpleOperationsAfterRun() {
        valkeyStandalone = builder().port(6381).build()
        valkeyStandalone!!.start()

        JedisPool("localhost", 6381).use { pool ->
            pool.getResource().use { jedis ->
                jedis.mset("abc", "1", "def", "2")
                Assertions.assertEquals("1", jedis.mget("abc").get(0))
                Assertions.assertEquals("2", jedis.mget("def").get(0))
                Assertions.assertNull(jedis.mget("xyz").get(0))
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun shouldIndicateInactiveBeforeStart() {
        valkeyStandalone = builder().port(6381).build()
        Assertions.assertFalse(valkeyStandalone!!.active)
    }

    @Test
    @Throws(IOException::class)
    fun shouldIndicateActiveAfterStart() {
        valkeyStandalone = builder().port(6381).build()
        valkeyStandalone!!.start()
        Assertions.assertTrue(valkeyStandalone!!.active)
    }

    @Test
    @Throws(IOException::class)
    fun shouldIndicateInactiveAfterStop() {
        valkeyStandalone = builder().port(6381).build()
        valkeyStandalone!!.start()
        valkeyStandalone!!.stop()
        Assertions.assertFalse(valkeyStandalone!!.active)
    }

    companion object {
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
        @Throws(IOException::class)
        private fun testReadyPattern(resourcePath: String, readyPattern: Pattern) {
            val `in` = ValkeyStandaloneTest::class.java.getResourceAsStream(resourcePath)
            Assertions.assertNotNull(`in`)
            BufferedReader(InputStreamReader(`in`)).use { reader ->
                var line: String?
                do {
                    line = reader.readLine()
                    Assertions.assertNotNull(line)
                } while (!readyPattern.matcher(line).matches())
            }
        }
    }
}
