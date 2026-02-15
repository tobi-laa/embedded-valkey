package io.github.tobi.laa.embedded.valkey.cluster.sharded

import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import io.github.tobi.laa.embedded.valkey.ports.PortProvider
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandaloneBuilder
import io.github.tobi.laa.embedded.valkey.testing.createInstallationSupplier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("Tests for ValkeyShardedClusterBuilder")
class ValkeyShardedClusterBuilderTest {

    @Test
    @DisplayName("Building should create main and replica nodes with cluster-enabled config")
    fun `build creates main and replica nodes`() {
        val os = detectOperatingSystem()
        val serverBuilder = ValkeyStandaloneBuilder()
            .installationSupplier(os, createInstallationSupplier(operatingSystem = os))
        val shardedCluster = ValkeyShardedClusterBuilder()
            .withServerBuilder(serverBuilder)
            .initializationTimeout(Duration.ofMillis(1))
            .shard("test-shard", 2)
            .build()

        assertThat(shardedCluster.mainNodes).hasSize(1)
        assertThat(shardedCluster.replicas).hasSize(2)
        assertThat(shardedCluster.nodes).hasSize(3)
        assertThat(shardedCluster.nodes).allSatisfy { node ->
            assertThat(node.config.directives("cluster-enabled")).isNotEmpty
        }
    }

    @Test
    @DisplayName("Building replicas should fail with IllegalStateException when main node entry is missing")
    fun `buildReplicas fails when main node entry is missing`() {
        val builder = ValkeyShardedClusterBuilder()
        val unmappedShard = Shard("test-shard", PortProvider(), 1)

        assertThrows(IllegalStateException::class.java) {
            builder.buildReplicas(unmappedShard)
        }
    }
}
