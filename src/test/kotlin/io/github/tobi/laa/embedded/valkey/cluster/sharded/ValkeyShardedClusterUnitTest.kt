package io.github.tobi.laa.embedded.valkey.cluster.sharded

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
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

    private var cluster: ValkeyShardedCluster? = null
    private var performAction: ThrowableAssert.ThrowingCallable? = null
    private var resolvedBuilder: ValkeyShardedClusterBuilder? = null
    private var caughtException: AtomicReference<Throwable?> = AtomicReference()
    private var mockJedisConstruction: MockedConstruction<Jedis>? = null
    private var mockStaticClusterClient: MockedStatic<RedisClusterClient>? = null

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { mainNode.port } returns 7000
        every { replicaNode.port } returns 7001
        cluster = null
        performAction = null
        resolvedBuilder = null
        caughtException = AtomicReference()
    }

    @AfterEach
    fun tearDown() {
        mockStaticClusterClient?.close()
        mockStaticClusterClient = null
        mockJedisConstruction?.close()
        mockJedisConstruction = null
    }

    @Test
    @DisplayName("Starting should link replicas to main nodes and configure shards")
    fun `start links replicas and shards`() {
        givenJedisMockedForSuccessfulClusterSetup()
        givenClusterWithTwoMainsAndOneReplica()
        whenClusterIsStarted()
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("Stopping should throw IOException when flushing nodes fails")
    fun `stop throws when flush fails`() {
        givenJedisFlushAllFails()
        givenSingleMainNodeCluster()
        whenStopIsCalled()
        thenIOExceptionIsThrown()
    }

    @Test
    @DisplayName("Stopping should succeed when all nodes stop cleanly")
    fun `stop succeeds when nodes stop cleanly`() {
        givenJedisFlushAllAndClusterResetSucceed()
        givenMainNodeStopsCleanly()
        givenSingleMainNodeCluster()
        whenStopIsCalled()
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("Stopping should wrap IOException from node stop into IOException")
    fun `stop wraps stop io exceptions`() {
        givenJedisFlushAllAndClusterResetSucceed()
        givenMainNodeStopThrows(IOException("boom"))
        givenSingleMainNodeCluster()
        whenStopIsCalled()
        thenIOExceptionIsThrown()
    }

    @Test
    @DisplayName("Stopping should wrap RuntimeException from node stop into IOException")
    fun `stop wraps stop runtime exceptions`() {
        givenJedisFlushAllAndClusterResetSucceed()
        givenMainNodeStopThrows(RuntimeException("boom"))
        givenSingleMainNodeCluster()
        whenStopIsCalled()
        thenIOExceptionIsThrown()
    }

    @Test
    @DisplayName("Server ports and role accessors should reflect the configured main and replica nodes")
    fun `serverPorts and role accessors reflect configured nodes`() {
        givenClusterWithMainAndReplica()
        thenServerPortsAre(7000, 7001)
        thenMainNodesAre(mainNode)
        thenReplicasAre(replicaNode)
    }

    @Test
    @DisplayName("Setting up replicas should fail when the main node ID is missing")
    fun `setupReplicas fails when main node id missing`() {
        givenClusterWithMainAndReplica()
        whenSetupReplicasIsCalled(7000)
        thenSetupExceptionIsThrown()
    }

    @Test
    @DisplayName("Waiting for cluster readiness should throw when the cluster never stabilizes")
    fun `waitForClusterToBeInteractReady throws when cluster never stabilizes`() {
        givenSingleMainNodeCluster()
        whenWaitForClusterToBeInteractReadyIsCalled()
        thenSetupExceptionIsThrown()
    }

    @Test
    @DisplayName("Companion builder should return a ValkeyShardedClusterBuilder")
    fun `companion builder returns builder`() {
        whenBuilderFactoryMethodIsCalled()
        thenResolvedBuilderIsValkeyShardedClusterBuilder()
    }

    @Test
    @DisplayName("Starting should stop cluster when linkReplicasAndShards fails")
    fun `start stops cluster on link failure`() {
        givenJedisClusterMyIdFails()
        givenMainNodeStopsCleanly()
        givenSingleMainNodeCluster()
        whenClusterIsStarted()
        thenRuntimeExceptionIsThrown()
    }

    @Test
    @DisplayName("Starting should wrap IOException from stop during link failure into RuntimeException")
    fun `start wraps io exception from stop during link failure`() {
        givenJedisClusterMyIdFails()
        givenMainNodeStopThrows(IOException("stop failed"))
        givenSingleMainNodeCluster()
        whenClusterIsStarted()
        thenRuntimeExceptionIsThrown()
    }

    @Test
    @DisplayName("Setting up replicas should throw when Jedis operations fail on the replica port")
    fun `setupReplicas throws when jedis fails on replica port`() {
        givenJedisMockedForMainPortSuccessReplicaPortFails()
        givenMainNodeStopsCleanly()
        givenClusterWithMainAndReplica()
        whenClusterIsStarted()
        thenRuntimeExceptionIsThrown()
    }

    @Test
    @DisplayName("waitForNodeToAppearInCluster should throw when node never appears before timeout")
    fun `waitForNodeToAppearInCluster throws on timeout`() {
        givenJedisClusterNodesReturnsOtherNodeId()
        givenSingleMainNodeCluster()
        whenWaitForNodeToAppearInClusterIsCalled("missing-node-id")
        thenSetupExceptionIsThrown()
    }

    @Test
    @DisplayName("waitForClusterToHaveStatusOK should throw when cluster never reaches OK status before timeout")
    fun `waitForClusterToHaveStatusOK throws on timeout`() {
        givenJedisClusterInfoReturnsFail()
        givenSingleMainNodeCluster()
        whenWaitForClusterToHaveStatusOKIsCalled()
        thenSetupExceptionIsThrown()
    }

    @Test
    @DisplayName("Waiting for predicate should throw setup exception when thread is interrupted")
    fun `waitForPredicateToPass throws on interruption`() {
        givenClusterWithInitializationTimeout(Duration.ofMillis(500))
        whenPredicateWaitIsRunInInterruptedThread()
        thenSetupExceptionWasCaughtInThread()
    }

    // --- given* ---

    private fun givenJedisMockedForSuccessfulClusterSetup() {
        val mockClusterClient = Mockito.mock(RedisClusterClient::class.java)
        Mockito.`when`(mockClusterClient.get(Mockito.anyString())).thenReturn("value")
        mockStaticClusterClient = Mockito.mockStatic(RedisClusterClient::class.java)
        mockStaticClusterClient!!.`when`<RedisClusterClient> {
            RedisClusterClient.create(Mockito.any(HostAndPort::class.java))
        }.thenReturn(mockClusterClient)
        mockJedisConstruction = Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.clusterMyId()).thenReturn("node-id")
            Mockito.`when`(mock.clusterNodes()).thenReturn("node-id")
            Mockito.`when`(mock.clusterInfo()).thenReturn("cluster_state:ok")
        }
    }

    private fun givenJedisFlushAllFails() {
        mockJedisConstruction = Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.flushAll()).thenThrow(RuntimeException("boom"))
            Mockito.`when`(mock.clusterReset(ClusterResetType.SOFT)).thenReturn("OK")
        }
    }

    private fun givenJedisFlushAllAndClusterResetSucceed() {
        mockJedisConstruction = Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.flushAll()).thenReturn("OK")
            Mockito.`when`(mock.clusterReset(ClusterResetType.SOFT)).thenReturn("OK")
        }
    }

    private fun givenJedisClusterMyIdFails() {
        mockJedisConstruction = Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.clusterMyId()).thenThrow(RuntimeException("connection refused"))
        }
    }

    private fun givenJedisClusterNodesReturnsOtherNodeId() {
        mockJedisConstruction = Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.clusterNodes()).thenReturn("other-node-id")
        }
    }

    private fun givenJedisClusterInfoReturnsFail() {
        mockJedisConstruction = Mockito.mockConstruction(Jedis::class.java) { mock, _ ->
            Mockito.`when`(mock.clusterInfo()).thenReturn("cluster_state:fail")
        }
    }

    private fun givenJedisMockedForMainPortSuccessReplicaPortFails() {
        mockJedisConstruction = Mockito.mockConstruction(Jedis::class.java) { mock, ctx ->
            val port = ctx.arguments()[1] as Int
            if (port == 7000) {
                Mockito.`when`(mock.clusterMyId()).thenReturn("main-node-id")
            } else {
                Mockito.`when`(mock.clusterMeet(anyString(), anyInt())).thenThrow(RuntimeException("connection refused"))
            }
        }
    }

    private fun givenMainNodeStopsCleanly() {
        every { mainNode.stop(any(), any(), any()) } just runs
    }

    private fun givenMainNodeStopThrows(exception: Exception) {
        every { mainNode.stop(any(), any(), any()) } throws exception
    }

    private fun givenClusterWithTwoMainsAndOneReplica() {
        val secondMain = mockk<ValkeyStandalone>(relaxed = true) { every { port } returns 7002 }
        cluster = ValkeyShardedCluster(
            nodes = listOf(mainNode, replicaNode, secondMain),
            replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf(7001), 7002 to mutableSetOf()),
            initializationTimeout = Duration.ZERO
        )
    }

    private fun givenSingleMainNodeCluster() {
        cluster = ValkeyShardedCluster(
            nodes = listOf(mainNode),
            replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
            initializationTimeout = Duration.ZERO
        )
    }

    private fun givenClusterWithMainAndReplica() {
        cluster = ValkeyShardedCluster(
            nodes = listOf(mainNode, replicaNode),
            replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf(7001)),
            initializationTimeout = Duration.ZERO
        )
    }

    private fun givenClusterWithInitializationTimeout(timeout: Duration) {
        cluster = ValkeyShardedCluster(
            nodes = listOf(mainNode),
            replicasPortsByMainNodePort = linkedMapOf(7000 to mutableSetOf()),
            initializationTimeout = timeout
        )
    }

    // --- when* ---

    private fun whenClusterIsStarted() {
        performAction = ThrowableAssert.ThrowingCallable {
            cluster!!.start(awaitReadiness = false, maxWaitTimeSeconds = 1)
        }
    }

    private fun whenStopIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            cluster!!.stop()
        }
    }

    private fun whenSetupReplicasIsCalled(mainPort: Int) {
        performAction = ThrowableAssert.ThrowingCallable {
            cluster!!.setupReplicas(mainPort)
        }
    }

    private fun whenWaitForClusterToBeInteractReadyIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            cluster!!.waitForClusterToBeInteractReady()
        }
    }

    private fun whenBuilderFactoryMethodIsCalled() {
        resolvedBuilder = ValkeyShardedCluster.builder()
    }

    private fun whenWaitForNodeToAppearInClusterIsCalled(nodeId: String) {
        val jedis = Jedis("127.0.0.1", 7000)
        performAction = ThrowableAssert.ThrowingCallable {
            cluster!!.waitForNodeToAppearInCluster(jedis, nodeId)
        }
    }

    private fun whenWaitForClusterToHaveStatusOKIsCalled() {
        val jedis = Jedis("127.0.0.1", 7000)
        performAction = ThrowableAssert.ThrowingCallable {
            cluster!!.waitForClusterToHaveStatusOK(jedis)
        }
    }

    private fun whenPredicateWaitIsRunInInterruptedThread() {
        val thread = Thread {
            try {
                cluster!!.waitForPredicateToPass(Supplier { false })
            } catch (e: ValkeyShardedClusterSetupException) {
                caughtException.set(e)
            }
        }
        thread.start()
        Thread.sleep(50)
        thread.interrupt()
        thread.join()
    }

    // --- then* ---

    private fun thenNoErrorOccurs() {
        assertThatCode(performAction!!).doesNotThrowAnyException()
    }

    private fun thenIOExceptionIsThrown() {
        assertThatCode(performAction!!).isInstanceOf(IOException::class.java)
    }

    private fun thenRuntimeExceptionIsThrown() {
        assertThatCode(performAction!!).isInstanceOf(RuntimeException::class.java)
    }

    private fun thenSetupExceptionIsThrown() {
        assertThatCode(performAction!!).isInstanceOf(ValkeyShardedClusterSetupException::class.java)
    }

    private fun thenServerPortsAre(vararg ports: Int) {
        assertThat(cluster!!.serverPorts()).containsExactly(*ports.toTypedArray())
    }

    private fun thenMainNodesAre(vararg nodes: ValkeyStandalone) {
        assertThat(cluster!!.mainNodes).containsExactly(*nodes)
    }

    private fun thenReplicasAre(vararg nodes: ValkeyStandalone) {
        assertThat(cluster!!.replicas).containsExactly(*nodes)
    }

    private fun thenResolvedBuilderIsValkeyShardedClusterBuilder() {
        assertThat(resolvedBuilder).isInstanceOf(ValkeyShardedClusterBuilder::class.java)
    }

    private fun thenSetupExceptionWasCaughtInThread() {
        assertThat(caughtException.get()).isInstanceOf(ValkeyShardedClusterSetupException::class.java)
    }
}
