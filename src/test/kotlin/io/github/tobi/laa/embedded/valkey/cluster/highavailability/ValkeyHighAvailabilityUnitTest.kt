package io.github.tobi.laa.embedded.valkey.cluster.highavailability

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.testing.createInstallationSupplier
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Unit tests for ValkeyHighAvailability")
class ValkeyHighAvailabilityUnitTest {

    private var performAction: ThrowableAssert.ThrowingCallable? = null
    private var builtHighAvailability: ValkeyHighAvailability? = null
    private var resolvedBuilder: ValkeyHighAvailabilityBuilder? = null

    @BeforeEach
    fun reset() {
        performAction = null
        builtHighAvailability = null
        resolvedBuilder = null
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
    fun `returns server ports, sentinel ports, and all nodes`() {
        givenBuiltHighAvailability(sentinelPort = 26379, serverPort = 6380)
        thenSentinelPortsContainExactly(26379)
        thenServerPortsContainExactly(6380)
        thenNodesAreSentinelsFollowedByServers()
    }

    @Test
    @DisplayName("promoteConfiguredMasters should skip servers with no port configured")
    fun `promoteConfiguredMasters skips server with no port configured`() {
        givenBuiltHighAvailabilityWithServerHavingNoPort()
        whenPromoteConfiguredMastersIsCalled()
        thenNoErrorOccurs()
    }

    private fun givenBuiltHighAvailability(sentinelPort: Int, serverPort: Int) {
        builtHighAvailability = ValkeyHighAvailability(listOf(sentinelWithPort(sentinelPort)), listOf(serverWithPort(serverPort)))
    }

    private fun givenBuiltHighAvailabilityWithServerHavingNoPort() {
        builtHighAvailability = ValkeyHighAvailability(
            listOf(sentinelWithPort(26379)),
            listOf(ValkeyStandalone(createInstallationSupplier(), ValkeyConfBuilder().build()))
        )
    }

    private fun sentinelWithPort(port: Int): ValkeySentinel {
        return ValkeySentinel(createInstallationSupplier(), ValkeyConfBuilder().port(port).build())
    }

    private fun serverWithPort(port: Int): ValkeyStandalone {
        return ValkeyStandalone(createInstallationSupplier(), ValkeyConfBuilder().port(port).build())
    }

    private fun whenHighAvailabilityIsConstructedWith(sentinels: List<ValkeySentinel>, servers: List<ValkeyStandalone>) {
        performAction = ThrowableAssert.ThrowingCallable {
            builtHighAvailability = ValkeyHighAvailability(sentinels, servers)
        }
    }

    private fun whenBuilderFactoryMethodIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            resolvedBuilder = ValkeyHighAvailability.builder()
        }
    }

    private fun whenPromoteConfiguredMastersIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            builtHighAvailability!!.promoteConfiguredMasters()
        }
    }

    private fun thenNoErrorOccurs() {
        assertThatCode(performAction!!).doesNotThrowAnyException()
    }

    private fun thenIllegalStateExceptionIsThrownContaining(message: String) {
        assertThatCode(performAction!!).isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining(message)
    }

    private fun thenSentinelPortsContainExactly(vararg ports: Int) {
        assertThat(builtHighAvailability!!.sentinelPorts()).containsExactly(*ports.toTypedArray())
    }

    private fun thenServerPortsContainExactly(vararg ports: Int) {
        assertThat(builtHighAvailability!!.serverPorts()).containsExactly(*ports.toTypedArray())
    }

    private fun thenNodesAreSentinelsFollowedByServers() {
        assertThat(builtHighAvailability!!.nodes)
            .isEqualTo(builtHighAvailability!!.sentinels + builtHighAvailability!!.servers)
    }

    private fun thenResolvedBuilderIsValkeyHighAvailabilityBuilder() {
        assertThat(resolvedBuilder).isInstanceOf(ValkeyHighAvailabilityBuilder::class.java)
    }
}
