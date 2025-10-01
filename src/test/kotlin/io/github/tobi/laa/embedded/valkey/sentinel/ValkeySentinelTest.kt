package io.github.tobi.laa.embedded.valkey.sentinel

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import org.junit.Assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool
import java.io.IOException
import java.util.Set
import java.util.concurrent.TimeUnit

internal class ValkeySentinelTest {
    private val bindAddress = "localhost"

    private var sentinel: ValkeySentinel? = null
    private var server: ValkeyStandalone? = null

    @AfterEach
    @Throws(IOException::class)
    fun stopSentinel() {
        try {
            if (sentinel != null && sentinel!!.active) {
                sentinel!!.stop()
            }
        } finally {
            if (server != null && server!!.active) {
                server!!.stop()
            }
        }
    }

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
        server = ValkeyStandalone.builder().build()
        sentinel = ValkeySentinel.builder().bind(bindAddress).build()
        server!!.start()
        sentinel!!.start()

        var pool: JedisSentinelPool? = null
        var jedis: Jedis? = null
        try {
            pool = JedisSentinelPool("mymain", Set.of<String?>(*arrayOf<String>("localhost:26379")))
            jedis = pool.getResource()
            jedis.mset("abc", "1", "def", "2")

            Assert.assertEquals("1", jedis.mget("abc").get(0))
            Assert.assertEquals("2", jedis.mget("def").get(0))
            Assert.assertNull(jedis.mget("xyz").get(0))
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
