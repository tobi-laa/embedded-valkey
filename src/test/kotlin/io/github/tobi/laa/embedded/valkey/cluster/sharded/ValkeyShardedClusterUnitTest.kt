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
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.Jedis
import redis.clients.jedis.RedisClusterClient
import redis.clients.jedis.args.ClusterResetType
import java.io.IOException
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
            val mockClusterClient = Mockito.mock(RedisClusterClient::class.java)
            Mockito.`when`(mockClusterClient.get(Mockito.anyString())).thenReturn("value")
            Mockito.mockStatic(RedisClusterClient::class.java).use { staticMock ->
                staticMock.`when`<RedisClusterClient> {
                    RedisClusterClient.create(Mockito.any(HostAndPort::class.java))
                }.thenReturn(mockClusterClient)
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

        assertThrows(ValkeyShardedClusterSetupException::class.java) {
            cluster.setupReplicas(7000)
        }
    }

    @Test
    @DisplayName("Waiting for cluster readiness should throw when the cluster never stabilizes")
    fun `waitForClusterToBeInteractReady throws when cluster never stabilizes`() {
        val cluster = ValkeyShardedCluster(
            nodes = listOf(mainNode),
            replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
            initializationTimeout = Duration.ZERO
        )

        assertThrows(ValkeyShardedClusterSetupException::class.java) {
            cluster.waitForClusterToBeInteractReady()
        }
    }

    @Test
    @DisplayName("Companion builder should return a ValkeyShardedClusterBuilder")
    fun `companion builder returns builder`() {
        assertThat(ValkeyShardedCluster.builder()).isInstanceOf(ValkeyShardedClusterBuilder::class.java)
    }

    @Test
    @DisplayName("Starting should stop cluster when linkReplicasAndShards fails")
    fun `start stops cluster on link failure`() {
        Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.clusterMyId()).thenThrow(RuntimeException("connection refused"))
        }.use {
            every { mainNode.stop(any(), any(), any()) } just runs
            val cluster = ValkeyShardedCluster(
                nodes = listOf(mainNode),
                replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
                initializationTimeout = Duration.ZERO
            )

            assertThrows(RuntimeException::class.java) { cluster.start(awaitReadiness = false, maxWaitTimeSeconds = 1) }
        }
    }

    @Test
    @DisplayName("waitForNodeToAppearInCluster should throw when node never appears")
    fun `waitForNodeToAppearInCluster throws on timeout`() {
        Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.clusterNodes()).thenReturn("other-node-id")
        }.use {
            val cluster = ValkeyShardedCluster(
                nodes = listOf(mainNode),
                replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
                initializationTimeout = Duration.ZERO
            )

            val jedis = Jedis("127.0.0.1", 7000)
            assertThrows(ValkeyShardedClusterSetupException::class.java) {
                cluster.waitForNodeToAppearInCluster(jedis, "missing-node-id")
            }
        }
    }

    @Test
    @DisplayName("waitForClusterToHaveStatusOK should throw when cluster never becomes ok")
    fun `waitForClusterToHaveStatusOK throws on timeout`() {
        Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.clusterInfo()).thenReturn("cluster_state:fail")
        }.use {
            val cluster = ValkeyShardedCluster(
                nodes = listOf(mainNode),
                replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
                initializationTimeout = Duration.ZERO
            )

            val jedis = Jedis("127.0.0.1", 7000)
            assertThrows(ValkeyShardedClusterSetupException::class.java) {
                cluster.waitForClusterToHaveStatusOK(jedis)
            }
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
        val failure = AtomicReference<Throwable?>()

        val thread = Thread {
            try {
                cluster.waitForPredicateToPass(Supplier { false })
            } catch (e: ValkeyShardedClusterSetupException) {
                failure.set(e)
            }
        }
        thread.start()
        Thread.sleep(50)
        thread.interrupt()
        thread.join()

        assertThat(failure.get()).isInstanceOf(ValkeyShardedClusterSetupException::class.java)
    }
}
