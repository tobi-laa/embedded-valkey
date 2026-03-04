package io.github.tobi.laa.embedded.valkey.cluster

import io.github.tobi.laa.embedded.valkey.IntegrationTest
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import redis.clients.jedis.RedisClient

@IntegrationTest
@DisplayName("Tests for Valkey server cluster replication")
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
    @DisplayName("It should be possible to perform simple read/write operations on a replicated cluster")
    fun testSimpleOperationsAfterRun() {
        RedisClient.create("localhost", 6300).use { redisClient ->
            redisClient.mset("abc", "1", "def", "2")

            Assertions.assertEquals("1", redisClient.mget("abc").get(0))
            Assertions.assertEquals("2", redisClient.mget("def").get(0))
            Assertions.assertNull(redisClient.mget("xyz").get(0))
        }
    }
}
