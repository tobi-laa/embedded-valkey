package io.github.tobi.laa.embedded.valkey.cluster.sharded

import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import io.github.tobi.laa.embedded.valkey.ports.PortProvider
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandaloneBuilder
import io.github.tobi.laa.embedded.valkey.testing.createInstallationSupplier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException
import java.time.Duration

@DisplayName("Tests for ValkeyShardedClusterBuilder")
class ValkeyShardedClusterBuilderTest {

    @Test
    @DisplayName("Building should create main and replica nodes with cluster-enabled config")
    fun `build creates main and replica nodes`() {
        val os = detectOperatingSystem()
        val serverBuilder = ValkeyStandaloneBuilder()
            .installationSupplier(os, createInstallationSupplier("#!/bin/sh\nsleep 1\n", os))
        val cluster = ValkeyShardedClusterBuilder()
            .withServerBuilder(serverBuilder)
            .initializationTimeout(Duration.ofMillis(1))
            .shard("s1", 2)
            .build()

        assertThat(cluster.mainNodes).hasSize(1)
        assertThat(cluster.replicas).hasSize(2)
        assertThat(cluster.nodes).hasSize(3)
        assertThat(cluster.nodes).allSatisfy { node ->
            assertThat(node.config.directives("cluster-enabled")).isNotEmpty
        }
    }

    @Test
    @DisplayName("Building replicas should fail with IllegalStateException when main node entry is missing")
    fun `buildReplicas fails when main node entry is missing`() {
        val builder = ValkeyShardedClusterBuilder()
        val shard = Shard("s1", PortProvider(), 1)
        val method = builder.javaClass.getDeclaredMethod("buildReplicas", Shard::class.java)
        method.isAccessible = true

        val exception = assertThrows(InvocationTargetException::class.java) {
            method.invoke(builder, shard)
        }

        assertThat(exception.targetException).isInstanceOf(IllegalStateException::class.java)
    }
}
