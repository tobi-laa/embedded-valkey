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

    private fun awaitMasterPool(masterName: String, sentinelHosts: Set<String>): JedisSentinelPool {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        var lastError: Exception? = null
        while (System.nanoTime() < deadline) {
            for (sentinelHost in sentinelHosts) {
                val (host, port) = sentinelHost.split(":", limit = 2)
                try {
                    Jedis(host, port.toInt()).use { sentinel ->
                        val masterAddr = sentinel.sentinelGetMasterAddrByName(masterName)
                        if (masterAddr != null && masterAddr.size >= 2) {
                            val masterHost = masterAddr[0]
                            val masterPort = masterAddr[1].toInt()
                            Jedis(masterHost, masterPort).use { master ->
                                master.set("__embedded_valkey_master_probe__", "1")
                                master.del("__embedded_valkey_master_probe__")
                            }
                            return JedisSentinelPool(masterName, sentinelHosts)
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
            TimeUnit.MILLISECONDS.sleep(250)
        }
        throw IllegalStateException("Sentinel did not provide a writable master for $masterName", lastError)
    }
}
