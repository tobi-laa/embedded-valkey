package io.github.tobi.laa.embedded.valkey.cluster.highavailability

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.testing.createInstallationSupplier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Unit tests for ValkeyHighAvailability")
class ValkeyHighAvailabilityUnitTest {

    @Test
    @DisplayName("Construction should require at least one sentinel")
    fun `requires at least one sentinel`() {
        val server = ValkeyStandalone(
            createInstallationSupplier("#!/bin/sh\nsleep 1\n"),
            ValkeyConfBuilder().port(6380).build()
        )

        assertThrows(IllegalStateException::class.java) { ValkeyHighAvailability(emptyList(), listOf(server)) }
    }

    @Test
    @DisplayName("Construction should require at least one server")
    fun `requires at least one server`() {
        val sentinel = ValkeySentinel(
            createInstallationSupplier("#!/bin/sh\nsleep 1\n"),
            ValkeyConfBuilder().port(26379).build()
        )

        assertThrows(IllegalStateException::class.java) { ValkeyHighAvailability(listOf(sentinel), emptyList()) }
    }

    @Test
    @DisplayName("Should return correct server ports, sentinel ports, and all nodes")
    fun `returns server and sentinel ports`() {
        val sentinel = ValkeySentinel(
            createInstallationSupplier("#!/bin/sh\nsleep 1\n"),
            ValkeyConfBuilder().port(26379).build()
        )
        val server = ValkeyStandalone(
            createInstallationSupplier("#!/bin/sh\nsleep 1\n"),
            ValkeyConfBuilder().port(6380).build()
        )

        val cluster = ValkeyHighAvailability(listOf(sentinel), listOf(server))

        assertThat(cluster.sentinelPorts()).containsExactly(26379)
        assertThat(cluster.serverPorts()).containsExactly(6380)
        assertThat(cluster.nodes).containsExactly(sentinel, server)
    }
}
