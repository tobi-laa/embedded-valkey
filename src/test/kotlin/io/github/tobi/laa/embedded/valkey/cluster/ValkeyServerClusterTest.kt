package io.github.tobi.laa.embedded.valkey.cluster

import io.github.tobi.laa.embedded.valkey.IntegrationTest
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

@IntegrationTest
class ValkeyServerClusterTest {
    private var valkeyStandalone1: ValkeyStandalone? = null
    private var valkeyStandalone2: ValkeyStandalone? = null

    @BeforeEach
    fun setUp() {
        valkeyStandalone1 = ValkeyStandalone.Companion.builder().port(6300).build()
        valkeyStandalone2 = ValkeyStandalone.Companion.builder().port(6301)
            .replicaOf("localhost", 6300)
            .build()

        valkeyStandalone1!!.start()
        valkeyStandalone2!!.start()
    }

    @Test
    fun testSimpleOperationsAfterRun() {
        var pool: JedisPool? = null
        var jedis: Jedis? = null
        try {
            pool = JedisPool("localhost", 6300)
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
        }
    }
}
