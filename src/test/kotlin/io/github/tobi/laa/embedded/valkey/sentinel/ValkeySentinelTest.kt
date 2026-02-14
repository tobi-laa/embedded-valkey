package io.github.tobi.laa.embedded.valkey.sentinel

import io.github.tobi.laa.embedded.valkey.IntegrationTest
import io.github.tobi.laa.embedded.valkey.cluster.highavailability.ReplicationGroup
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.ports.PortProvider
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool
import java.io.IOException
import java.util.concurrent.TimeUnit

@IntegrationTest
internal class ValkeySentinelTest {
    private val bindAddress = "localhost"

    private var sentinel: ValkeySentinel? = null
    private var server: ValkeyStandalone? = null

    @Test
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
            pool = awaitMasterPool("mymain", setOf("localhost:26379"))
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

    private fun awaitMasterPool(masterName: String, sentinelHosts: Set<String>): JedisSentinelPool {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        var lastError: Exception? = null
        while (System.nanoTime() < deadline) {
            var pool: JedisSentinelPool? = null
            var keepPool = false
            try {
                pool = JedisSentinelPool(masterName, sentinelHosts)
                pool.resource.use { jedis ->
                    if (jedis.role().firstOrNull()?.toString() == "master") {
                        keepPool = true
                        return pool
                    }
                }
            } catch (e: Exception) {
                lastError = e
            } finally {
                if (!keepPool) {
                    pool?.destroy()
                }
            }
            TimeUnit.MILLISECONDS.sleep(250)
        }
        throw IllegalStateException("Sentinel did not provide a writable master for $masterName", lastError)
    }
}
