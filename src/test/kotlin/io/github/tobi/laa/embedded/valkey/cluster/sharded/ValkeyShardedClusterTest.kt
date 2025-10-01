package io.github.tobi.laa.embedded.valkey.cluster.sharded

import org.junit.Assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import java.io.IOException
import java.nio.file.Path

internal class ValkeyShardedClusterTest {
    private var cluster: ValkeyShardedCluster? = null

    @AfterEach
    @Throws(IOException::class)
    fun stopCluster() {
        if (cluster != null) {
            cluster!!.stop()
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp() {
        cluster = ValkeyShardedCluster.Companion.builder()
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
            Assert.assertEquals("the value should be equal", "somevalue", jc.get("somekey"))
        }
    }

    @Test
    @Throws(IOException::class)
    fun testSimpleOperationsAfterClusterWithEphemeralPortsStart() {
        cluster!!.stop()
        cluster = ValkeyShardedCluster.Companion.builder()
            .shard("master1", 1)
            .shard("master2", 1)
            .shard("master3", 1)
            .build()
        cluster!!.start()
        JedisCluster(HostAndPort("127.0.0.1", cluster!!.nodes.get(0).port)).use { jc ->
            jc.set("somekey", "somevalue")
            Assert.assertEquals("the value should be equal", "somevalue", jc.get("somekey"))
        }
    }

    @Test
    @Throws(IOException::class)
    fun shouldAllowSubsequentRuns() {
        cluster!!.stop()
        cluster!!.start()
        JedisCluster(HostAndPort("127.0.0.1", cluster!!.nodes.get(0).port)).use { jc ->
            jc.set("somekey", "somevalue")
            Assert.assertEquals("the value should be equal", "somevalue", jc.get("somekey"))
        }
    } //    @Test
    //    public void shouldAllowSubsequentRunsInSameDirectory() throws IOException {
    //        cluster.stop();
    //        //
    //        File folder = temporaryFolder.newFolder();
    //        cluster = newRedisCluster()
    //                .withServerBuilder(RedisServer
    //                        .newRedisServer()
    //                        .executableProvider(newJarResourceProvider(folder)))
    //                .shard("master1", 1)
    //                .shard("master2", 1)
    //                .shard("master3", 1)
    //                .build();
    //        cluster.start();
    //        cluster.stop();
    //        cluster.start();
    //        try (final JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", cluster.getPort()))) {
    //            jc.set("somekey", "somevalue");
    //            assertEquals("the value should be equal", "somevalue", jc.get("somekey"));
    //        }
    //    }

    companion object {
        @TempDir
        var temporaryFolder: Path? = null
    }
}
