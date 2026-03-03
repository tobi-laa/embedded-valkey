package io.github.tobi.laa.embedded.valkey.cluster.highavailability

import io.github.tobi.laa.embedded.valkey.IntegrationTest
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.testing.awaitMasterPool
import io.github.tobi.laa.embedded.valkey.testing.createInstallationSupplier
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool

@IntegrationTest
@DisplayName("Tests for ValkeyHighAvailability")
internal class ValkeyHighAvailabilityTest {

    private var performAction: ThrowableAssert.ThrowingCallable? = null
    private var cluster: ValkeyHighAvailability? = null
    private var resolvedBuilder: ValkeyHighAvailabilityBuilder? = null
    private var sentinelHosts: Set<String>? = null
    private var pool: JedisSentinelPool? = null
    private var jedis: Jedis? = null

    @BeforeEach
    fun reset() {
        performAction = null
        cluster = null
        resolvedBuilder = null
        sentinelHosts = null
        pool = null
        jedis = null
    }

    @AfterEach
    fun cleanUpClientResources() {
        jedis?.close()
        pool?.destroy()
    }

    @Test
    @DisplayName("Construction should require at least one sentinel")
    fun `requires at least one sentinel`() {
        whenHighAvailabilityIsConstructedWith(sentinels = emptyList(), servers = listOf(serverWithPort(6380)))
        thenIllegalStateExceptionIsThrownContaining("At least one sentinel")
    }

    @Test
    @DisplayName("Construction should require at least one server")
    fun `requires at least one server`() {
        whenHighAvailabilityIsConstructedWith(sentinels = listOf(sentinelWithPort(26379)), servers = emptyList())
        thenIllegalStateExceptionIsThrownContaining("At least one server")
    }

    @Test
    @DisplayName("Companion builder should return a ValkeyHighAvailabilityBuilder instance")
    fun `companion builder returns builder`() {
        whenBuilderFactoryMethodIsCalled()
        thenNoErrorOccurs()
        thenResolvedBuilderIsValkeyHighAvailabilityBuilder()
    }

    @Test
    @DisplayName("Should return the correct sentinel ports, server ports, and all nodes in sentinels-first order")
    fun `returns correct sentinel ports, server ports, and all nodes`() {
        givenBuiltCluster(sentinelPort = 26379, serverPort = 6380)
        thenSentinelPortsContainExactly(26379)
        thenServerPortsContainExactly(6380)
        thenNodesAreSentinelsFollowedByServers()
    }

    @Test
    @DisplayName("promoteConfiguredMasters should skip servers with no port configured")
    fun `promoteConfiguredMasters skips server with no port configured`() {
        givenBuiltClusterWithServerHavingNoPort()
        whenPromoteConfiguredMastersIsCalled()
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("It should be possible to read and write data with a single master and no replicas")
    fun `supports read and write with single master and no replicas`() {
        givenStartedCluster(sentinelCount = 1, masterName = "ourmaster", replicaCount = 0)
        whenDataIsReadAndWrittenVia(masterName = "ourmaster")
        thenReadDataMatchesWritten()
    }

    @Test
    @DisplayName("It should be possible to read and write data with a single master and one replica")
    fun `supports read and write with single master and one replica`() {
        givenStartedCluster(sentinelCount = 1, masterName = "ourmaster", replicaCount = 1)
        whenDataIsReadAndWrittenVia(masterName = "ourmaster")
        thenReadDataMatchesWritten()
    }

    @Test
    @DisplayName("It should be possible to read and write data with a single master and two replicas")
    fun `supports read and write with single master and two replicas`() {
        givenStartedCluster(sentinelCount = 1, masterName = "ourmaster", replicaCount = 2)
        whenDataIsReadAndWrittenVia(masterName = "ourmaster")
        thenReadDataMatchesWritten()
    }

    @Test
    @DisplayName("It should be possible to read and write data with two sentinels, one master, and two replicas")
    fun `supports read and write with two sentinels, one master, and two replicas`() {
        givenStartedCluster(sentinelCount = 2, masterName = "ourmaster", replicaCount = 2)
        whenDataIsReadAndWrittenVia(masterName = "ourmaster")
        thenReadDataMatchesWritten()
    }

    @Test
    @DisplayName("It should be possible to read and write data when sentinel ports are assigned ephemerally")
    fun `supports read and write with ephemeral sentinel ports`() {
        givenStartedCluster(masterName = "ourmaster", replicaCount = 2)
        whenDataIsReadAndWrittenVia(masterName = "ourmaster")
        thenReadDataMatchesWritten()
    }

    @Test
    @DisplayName("It should be possible to read and write data with three sentinels and three replication groups")
    fun `supports read and write with three sentinels and three replication groups`() {
        givenStartedCluster(
            sentinelCount = 3,
            quorumSize = 2,
            groups = listOf("master1" to 1, "master2" to 1, "master3" to 1)
        )
        thenDataCanBeReadAndWrittenOnAllGroups("master1", "master2", "master3")
    }

    // --- given* ---

    private fun givenBuiltCluster(sentinelPort: Int, serverPort: Int) {
        cluster = ValkeyHighAvailability(
            listOf(sentinelWithPort(sentinelPort)),
            listOf(serverWithPort(serverPort))
        )
    }

    private fun givenBuiltClusterWithServerHavingNoPort() {
        cluster = ValkeyHighAvailability(
            listOf(sentinelWithPort(26379)),
            listOf(ValkeyStandalone(createInstallationSupplier(), ValkeyConfBuilder().build()))
        )
    }

    private fun givenStartedCluster(sentinelCount: Int = 1, masterName: String, replicaCount: Int) {
        cluster = ValkeyHighAvailability.builder()
            .sentinelCount(sentinelCount)
            .replicationGroup(masterName, replicaCount)
            .build()
        cluster!!.start()
        sentinelHosts = cluster!!.sentinelPorts().map { "localhost:$it" }.toSet()
    }

    private fun givenStartedCluster(sentinelCount: Int, quorumSize: Int, groups: List<Pair<String, Int>>) {
        val builder = ValkeyHighAvailability.builder()
            .sentinelCount(sentinelCount)
            .quorumSize(quorumSize)
        groups.forEach { (masterName, replicaCount) -> builder.replicationGroup(masterName, replicaCount) }
        cluster = builder.build()
        cluster!!.start()
        sentinelHosts = cluster!!.sentinelPorts().map { "localhost:$it" }.toSet()
    }

    private fun sentinelWithPort(port: Int): ValkeySentinel =
        ValkeySentinel(createInstallationSupplier(), ValkeyConfBuilder().port(port).build())

    private fun serverWithPort(port: Int): ValkeyStandalone =
        ValkeyStandalone(createInstallationSupplier(), ValkeyConfBuilder().port(port).build())

    // --- when* ---

    private fun whenHighAvailabilityIsConstructedWith(sentinels: List<ValkeySentinel>, servers: List<ValkeyStandalone>) {
        performAction = ThrowableAssert.ThrowingCallable {
            cluster = ValkeyHighAvailability(sentinels, servers)
        }
    }

    private fun whenBuilderFactoryMethodIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            resolvedBuilder = ValkeyHighAvailability.builder()
        }
    }

    private fun whenPromoteConfiguredMastersIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            cluster!!.promoteConfiguredMasters()
        }
    }

    private fun whenDataIsReadAndWrittenVia(masterName: String) {
        pool = awaitMasterPool(masterName, sentinelHosts!!)
        jedis = pool!!.resource
        jedis!!.mset("abc", "1", "def", "2")
    }

    // --- then* ---

    private fun thenNoErrorOccurs() {
        assertThatCode(performAction!!).doesNotThrowAnyException()
    }

    private fun thenIllegalStateExceptionIsThrownContaining(message: String) {
        assertThatCode(performAction!!).isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining(message)
    }

    private fun thenSentinelPortsContainExactly(vararg ports: Int) {
        assertThat(cluster!!.sentinelPorts()).containsExactly(*ports.toTypedArray())
    }

    private fun thenServerPortsContainExactly(vararg ports: Int) {
        assertThat(cluster!!.serverPorts()).containsExactly(*ports.toTypedArray())
    }

    private fun thenNodesAreSentinelsFollowedByServers() {
        assertThat(cluster!!.nodes).isEqualTo(cluster!!.sentinels + cluster!!.servers)
    }

    private fun thenResolvedBuilderIsValkeyHighAvailabilityBuilder() {
        assertThat(resolvedBuilder).isInstanceOf(ValkeyHighAvailabilityBuilder::class.java)
    }

    private fun thenReadDataMatchesWritten() {
        assertThat(jedis!!.mget("abc")[0]).isEqualTo("1")
        assertThat(jedis!!.mget("def")[0]).isEqualTo("2")
        assertThat(jedis!!.mget("xyz")[0]).isNull()
    }

    private fun thenDataCanBeReadAndWrittenOnAllGroups(vararg masterNames: String) {
        for (masterName in masterNames) {
            awaitMasterPool(masterName, sentinelHosts!!).use { masterPool ->
                masterPool.resource.use { masterJedis ->
                    masterJedis.mset("abc", "1", "def", "2")
                    assertThat(masterJedis.mget("abc")[0]).isEqualTo("1")
                    assertThat(masterJedis.mget("def")[0]).isEqualTo("2")
                    assertThat(masterJedis.mget("xyz")[0]).isNull()
                }
            }
        }
    }
}
