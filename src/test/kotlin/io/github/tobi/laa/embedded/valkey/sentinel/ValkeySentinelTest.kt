package io.github.tobi.laa.embedded.valkey.sentinel

import io.github.tobi.laa.embedded.valkey.IntegrationTest
import io.github.tobi.laa.embedded.valkey.cluster.highavailability.ReplicationGroup
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.ports.PortProvider
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.github.tobi.laa.embedded.valkey.testing.awaitMasterPool
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool
import java.io.IOException
import java.util.concurrent.TimeUnit

@IntegrationTest
@DisplayName("Integration tests for ValkeySentinel")
internal class ValkeySentinelTest {
    private val bindAddress = "localhost"

    private var sentinel: ValkeySentinel? = null
    private var server: ValkeyStandalone? = null

    @Test
    @DisplayName("Starting and stopping a sentinel with a server should work without errors")
    @Throws(InterruptedException::class, IOException::class)
    fun testSimpleRun() {
        server = ValkeyStandalone.builder().build()
        sentinel = ValkeySentinel.builder().bind(bindAddress).build()
        sentinel!!.start()
        server!!.start()
        TimeUnit.SECONDS.sleep(1)
        server!!.stop()
        sentinel!!.stop()
    }

    @Test
    @DisplayName("Multiple start/stop cycles of the sentinel should work without errors")
    @Throws(IOException::class)
    fun shouldAllowSubsequentRuns() {
        sentinel = ValkeySentinel.builder().bind(bindAddress).build()
        sentinel!!.start()
        sentinel!!.stop()

        sentinel!!.start()
        sentinel!!.stop()

        sentinel!!.start()
        sentinel!!.stop()
    }

    @Test
    @DisplayName("It should be possible to read and write data via a sentinel-monitored master")
    @Throws(IOException::class)
    fun testSimpleOperationsAfterRun() {
        val portProvider = PortProvider()
        val serverPort = portProvider.next()
        server = ValkeyStandalone.builder().port(serverPort).build()
        val sentinelBuilder = ValkeySentinel.builder().bind(bindAddress)
        sentinelBuilder.monitor(ReplicationGroup("mymain", serverPort, emptyList()))
        sentinel = sentinelBuilder.build()
        server!!.start()
        sentinel!!.start()

        var pool: JedisSentinelPool? = null
        var jedis: Jedis? = null
        try {
            pool = awaitMasterPool("mymain", setOf("localhost:${sentinel!!.port}"))
            jedis = pool.getResource()
            jedis.mset("abc", "1", "def", "2")

            Assertions.assertEquals("1", jedis.mget("abc").get(0))
            Assertions.assertEquals("2", jedis.mget("def").get(0))
            Assertions.assertNull(jedis.mget("xyz").get(0))
        } finally {
            if (jedis != null) {
                jedis.close()
            }
            if (pool != null) {
                pool.destroy()
            }
            sentinel!!.stop()
            server!!.stop()
        }
    }

}
