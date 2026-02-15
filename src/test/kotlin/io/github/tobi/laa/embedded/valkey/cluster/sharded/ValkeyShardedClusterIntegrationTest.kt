package io.github.tobi.laa.embedded.valkey.cluster.sharded

import io.github.tobi.laa.embedded.valkey.IntegrationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import java.io.IOException
import java.nio.file.Path

@IntegrationTest
@DisplayName("Integration tests for ValkeyShardedCluster")
internal class ValkeyShardedClusterIntegrationTest {

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
    @DisplayName("It should be possible to read and write data after the cluster has started")
    fun testSimpleOperationsAfterClusterStart() {
        JedisCluster(HostAndPort("127.0.0.1", cluster!!.nodes.get(0).port)).use { jedisCluster ->
            jedisCluster.set("somekey", "somevalue")
            Assertions.assertEquals("somevalue", jedisCluster.get("somekey"))
        }
    }

    @Test
    @DisplayName("It should be possible to read and write data when using ephemeral ports")
    @Throws(IOException::class)
    fun testSimpleOperationsAfterClusterWithEphemeralPortsStart() {
        cluster!!.stop()
        cluster = ValkeyShardedCluster.builder()
            .shard("master1", 1)
            .shard("master2", 1)
            .shard("master3", 1)
            .build()
        cluster!!.start()
        JedisCluster(HostAndPort("127.0.0.1", cluster!!.nodes.get(0).port)).use { jedisCluster ->
            jedisCluster.set("somekey", "somevalue")
            Assertions.assertEquals("somevalue", jedisCluster.get("somekey"))
        }
    }

    @Test
    @DisplayName("Stopping and restarting the cluster should work without errors")
    @Throws(IOException::class)
    fun shouldAllowSubsequentRuns() {
        cluster!!.stop()
        cluster!!.start()
        JedisCluster(HostAndPort("127.0.0.1", cluster!!.nodes.get(0).port)).use { jedisCluster ->
            jedisCluster.set("somekey", "somevalue")
            Assertions.assertEquals("somevalue", jedisCluster.get("somekey"))
        }
    }

    @Test
    @DisplayName("Multiple start/stop cycles in the same directory should work without errors")
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
        JedisCluster(HostAndPort("127.0.0.1", cluster!!.nodes.get(0).port)).use { jedisCluster ->
            jedisCluster.set("somekey", "somevalue")
            Assertions.assertEquals("somevalue", jedisCluster.get("somekey"))
        }
    }

}
