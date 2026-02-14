package io.github.tobi.laa.embedded.valkey.cluster.sharded

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.args.ClusterResetType
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

@DisplayName("Unit tests for ValkeyShardedCluster")
class ValkeyShardedClusterUnitTest {

    @MockK(relaxed = true)
    private lateinit var mainNode: ValkeyStandalone

    @MockK(relaxed = true)
    private lateinit var replicaNode: ValkeyStandalone

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { mainNode.port } returns 7000
        every { replicaNode.port } returns 7001
    }

    @Test
    @DisplayName("Starting should link replicas to main nodes and configure shards")
    fun `start links replicas and shards`() {
        Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.clusterMyId()).thenReturn("node-id")
            Mockito.`when`(mock.clusterNodes()).thenReturn("node-id")
            Mockito.`when`(mock.clusterInfo()).thenReturn("cluster_state:ok")
        }.use {
            Mockito.mockConstruction(JedisCluster::class.java) { mock, _ ->
                Mockito.`when`(mock.get(Mockito.anyString())).thenReturn("value")
            }.use {
            val secondMain = mockk<ValkeyStandalone>(relaxed = true) {
                every { port } returns 7002
            }
            val cluster = ValkeyShardedCluster(
                nodes = listOf(mainNode, replicaNode, secondMain),
                replicasPortsByMainNodePort = linkedMapOf(
                    7000 to mutableSetOf(7001),
                    7002 to mutableSetOf()
                ),
                initializationTimeout = Duration.ZERO
            )

            cluster.start(awaitReadiness = false, maxWaitTimeSeconds = 1)
            }
        }
    }

    @Test
    @DisplayName("Stopping should throw IOException when flushing nodes fails")
    fun `stop throws when flush fails`() {
        Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.flushAll()).thenThrow(RuntimeException("boom"))
            Mockito.`when`(mock.clusterReset(ClusterResetType.SOFT)).thenReturn("OK")
        }.use {
            val cluster = ValkeyShardedCluster(
                nodes = listOf(mainNode),
                replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
                initializationTimeout = Duration.ZERO
            )

            assertThrows(IOException::class.java) { cluster.stop() }
        }
    }

    @Test
    @DisplayName("Stopping should succeed when all nodes stop cleanly")
    fun `stop succeeds when nodes stop cleanly`() {
        Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.flushAll()).thenReturn("OK")
            Mockito.`when`(mock.clusterReset(ClusterResetType.SOFT)).thenReturn("OK")
        }.use {
            every { mainNode.stop(any(), any(), any()) } just runs
            val cluster = ValkeyShardedCluster(
                nodes = listOf(mainNode),
                replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
                initializationTimeout = Duration.ZERO
            )

            cluster.stop()
        }
    }

    @Test
    @DisplayName("Stopping should wrap IOException from node stop into IOException")
    fun `stop wraps stop io exceptions`() {
        Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.flushAll()).thenReturn("OK")
            Mockito.`when`(mock.clusterReset(ClusterResetType.SOFT)).thenReturn("OK")
        }.use {
            every { mainNode.stop(any(), any(), any()) } throws IOException("boom")
            val cluster = ValkeyShardedCluster(
                nodes = listOf(mainNode),
                replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
                initializationTimeout = Duration.ZERO
            )

            assertThrows(IOException::class.java) { cluster.stop() }
        }
    }

    @Test
    @DisplayName("Stopping should wrap RuntimeException from node stop into IOException")
    fun `stop wraps stop runtime exceptions`() {
        Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.flushAll()).thenReturn("OK")
            Mockito.`when`(mock.clusterReset(ClusterResetType.SOFT)).thenReturn("OK")
        }.use {
            every { mainNode.stop(any(), any(), any()) } throws RuntimeException("boom")
            val cluster = ValkeyShardedCluster(
                nodes = listOf(mainNode),
                replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
                initializationTimeout = Duration.ZERO
            )

            assertThrows(IOException::class.java) { cluster.stop() }
        }
    }

    @Test
    @DisplayName("Server ports and role accessors should reflect the configured main and replica nodes")
    fun `serverPorts and role accessors reflect configured nodes`() {
        val main = mockk<ValkeyStandalone>(relaxed = true) {
            every { port } returns 6380
        }
        val replica = mockk<ValkeyStandalone>(relaxed = true) {
            every { port } returns 6381
        }
        val cluster = ValkeyShardedCluster(
            nodes = listOf(main, replica),
            replicasPortsByMainNodePort = linkedMapOf(6380 to mutableSetOf(6381)),
            initializationTimeout = Duration.ZERO
        )

        assertThat(cluster.serverPorts()).containsExactly(6380, 6381)
        assertThat(cluster.mainNodes).containsExactly(main)
        assertThat(cluster.replicas).containsExactly(replica)
    }

    @Test
    @DisplayName("Setting up replicas should fail when the main node ID is missing")
    fun `setupReplicas fails when main node id missing`() {
        val cluster = ValkeyShardedCluster(
            nodes = listOf(mainNode, replicaNode),
            replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf(7001)),
            initializationTimeout = Duration.ZERO
        )

        val method = cluster.javaClass.getDeclaredMethod("setupReplicas", Int::class.java)
        method.isAccessible = true
        val exception = assertThrows(InvocationTargetException::class.java) {
            method.invoke(cluster, 7000)
        }

        assertThat(exception.targetException).isInstanceOf(ValkeyShardedClusterSetupException::class.java)
    }

    @Test
    @DisplayName("Waiting for cluster readiness should throw when the cluster never stabilizes")
    fun `waitForClusterToBeInteractReady throws when cluster never stabilizes`() {
        Mockito.mockConstruction(JedisCluster::class.java) { mock, _ ->
            Mockito.`when`(mock.get(Mockito.anyString())).thenThrow(RuntimeException("down"))
        }.use {
            val cluster = ValkeyShardedCluster(
                nodes = listOf(mainNode),
                replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
                initializationTimeout = Duration.ZERO
            )

            val method = cluster.javaClass.getDeclaredMethod("waitForClusterToBeInteractReady")
            method.isAccessible = true
            val exception = assertThrows(InvocationTargetException::class.java) {
                method.invoke(cluster)
            }

            assertThat(exception.targetException).isInstanceOf(ValkeyShardedClusterSetupException::class.java)
        }
    }

    @Test
    @DisplayName("Waiting for predicate should throw setup exception when thread is interrupted")
    fun `waitForPredicateToPass throws on interruption`() {
        val cluster = ValkeyShardedCluster(
            nodes = listOf(mainNode),
            replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
            initializationTimeout = Duration.ofMillis(500)
        )
        val method = cluster.javaClass.getDeclaredMethod("waitForPredicateToPass", Supplier::class.java)
        method.isAccessible = true
        val failure = AtomicReference<Throwable?>()

        val thread = Thread {
            try {
                method.invoke(cluster, Supplier { false })
            } catch (e: InvocationTargetException) {
                failure.set(e.targetException)
            }
        }
        thread.start()
        Thread.sleep(50)
        thread.interrupt()
        thread.join()

        assertThat(failure.get()).isInstanceOf(ValkeyShardedClusterSetupException::class.java)
    }
}
