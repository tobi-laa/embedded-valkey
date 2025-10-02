package io.github.tobi.laa.embedded.valkey.cluster.sharded

import io.github.tobi.laa.embedded.valkey.IntegrationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import java.io.IOException
import java.nio.file.Path

@IntegrationTest
internal class ValkeyShardedClusterTest {

    private var cluster: ValkeyShardedCluster? = null

    @TempDir
    private var temporaryFolder: Path? = null

    @BeforeEach
    @Throws(IOException::class)
    fun setUp() {
        cluster = ValkeyShardedCluster.builder()
            .shard("master1", 1)
            .shard("master2", 1)
            .shard("master3", 1)
            .build()
        cluster!!.start()
    }

    @Test
    fun testSimpleOperationsAfterClusterStart() {
        JedisCluster(HostAndPort("127.0.0.1", cluster!!.nodes.get(0).port)).use { jc ->
            jc.set("somekey", "somevalue")
            Assertions.assertEquals("somevalue", jc.get("somekey"))
        }
    }

    @Test
    @Throws(IOException::class)
    fun testSimpleOperationsAfterClusterWithEphemeralPortsStart() {
        cluster!!.stop()
        cluster = ValkeyShardedCluster.builder()
            .shard("master1", 1)
            .shard("master2", 1)
            .shard("master3", 1)
            .build()
        cluster!!.start()
        JedisCluster(HostAndPort("127.0.0.1", cluster!!.nodes.get(0).port)).use { jc ->
            jc.set("somekey", "somevalue")
            Assertions.assertEquals("somevalue", jc.get("somekey"))
        }
    }

    @Test
    @Throws(IOException::class)
    fun shouldAllowSubsequentRuns() {
        cluster!!.stop()
        cluster!!.start()
        JedisCluster(HostAndPort("127.0.0.1", cluster!!.nodes.get(0).port)).use { jc ->
            jc.set("somekey", "somevalue")
            Assertions.assertEquals("somevalue", jc.get("somekey"))
        }
    }

    @Test
    fun shouldAllowSubsequentRunsInSameDirectory() {
        cluster!!.stop()
        cluster = ValkeyShardedCluster.builder()
            .shard("master1", 1)
            .shard("master2", 1)
            .shard("master3", 1)
            .build()
        cluster!!.start()
        cluster!!.stop()
        cluster!!.start()
        JedisCluster(HostAndPort("127.0.0.1", cluster!!.nodes.get(0).port)).use { jc ->
            jc.set("somekey", "somevalue")
            Assertions.assertEquals("somevalue", jc.get("somekey"))
        }
    }

}
