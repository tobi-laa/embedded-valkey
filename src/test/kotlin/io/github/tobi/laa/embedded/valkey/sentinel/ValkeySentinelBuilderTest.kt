package io.github.tobi.laa.embedded.valkey.sentinel

import io.github.tobi.laa.embedded.valkey.cluster.highavailability.ReplicationGroup
import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import io.github.tobi.laa.embedded.valkey.testing.createInstallationSupplier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests for ValkeySentinelBuilder")
class ValkeySentinelBuilderTest {

    @Test
    @DisplayName("Building should add a default monitor when no replication groups are configured")
    fun `build adds default monitor when no replication groups configured`() {
        val os = detectOperatingSystem()
        val builder = ValkeySentinelBuilder()
            .installationSupplier(os, createInstallationSupplier("#!/bin/sh\nsleep 1\n", os))

        val sentinel = builder.build()

        assertThat(sentinel.config.directives("sentinel")).anySatisfy { directive ->
            assertThat(directive.arguments).contains("monitor", "mymain", "127.0.0.1")
        }
        assertThat(sentinel.config.binds()).contains("::1", "127.0.0.1")
        assertThat(sentinel.config.port()).isNotNull
    }

    @Test
    @DisplayName("Cloning should preserve replication groups and custom configuration")
    fun `clone preserves replication groups and configuration`() {
        val os = detectOperatingSystem()
        val builder = ValkeySentinelBuilder()
            .installationSupplier(os, createInstallationSupplier("#!/bin/sh\nsleep 1\n", os))
            .bind("0.0.0.0")
            .port(26390)
            .quorumSize(2)
        builder.monitor(ReplicationGroup("main", 6380, listOf(6381, 6382)))

        val cloned = builder.clone()
        val sentinel = cloned.build()

        val directives = sentinel.config.directives("sentinel")
        assertThat(directives).anySatisfy { directive ->
            assertThat(directive.arguments).contains("monitor", "main", "127.0.0.1", "6380", "2")
        }
        assertThat(sentinel.config.binds()).contains("0.0.0.0")
        assertThat(sentinel.config.port()).isEqualTo(26390)
    }

    @Test
    @DisplayName("Building with replication groups should add monitors for each group")
    fun `build with replication groups adds monitors`() {
        val os = detectOperatingSystem()
        val builder = ValkeySentinelBuilder()
            .installationSupplier(os, createInstallationSupplier("#!/bin/sh\nsleep 1\n", os))
        builder.monitor(ReplicationGroup("master1", 6380, listOf(6381)))
        builder.monitor(ReplicationGroup("master2", 6382, emptyList()))

        val sentinel = builder.build()

        val directives = sentinel.config.directives("sentinel")
        assertThat(directives).anySatisfy { d ->
            assertThat(d.arguments).contains("monitor", "master1", "127.0.0.1", "6380")
        }
        assertThat(directives).anySatisfy { d ->
            assertThat(d.arguments).contains("monitor", "master2", "127.0.0.1", "6382")
        }
    }

    @Test
    @DisplayName("Builder setter methods should be chainable")
    fun `setter methods are chainable`() {
        val os = detectOperatingSystem()
        val builder = ValkeySentinelBuilder()
            .installationSupplier(os, createInstallationSupplier("#!/bin/sh\nsleep 1\n", os))
            .downAfterMilliseconds(5000)
            .failOverTimeout(10000)
            .parallelSyncs(2)
            .directive("loglevel", "debug")

        val sentinel = builder.build()

        val directives = sentinel.config.directives("sentinel")
        assertThat(directives).anySatisfy { d ->
            assertThat(d.arguments).contains("down-after-milliseconds", "mymain", "5000")
        }
        assertThat(directives).anySatisfy { d ->
            assertThat(d.arguments).contains("failover-timeout", "mymain", "10000")
        }
    }

    @Test
    @DisplayName("ValkeySentinel should report inactive when not started")
    fun `sentinel reports inactive when not started`() {
        val os = detectOperatingSystem()
        val sentinel = ValkeySentinelBuilder()
            .installationSupplier(os, createInstallationSupplier("#!/bin/sh\nsleep 1\n", os))
            .build()

        assertThat(sentinel.active).isFalse()
    }

    @Test
    @DisplayName("ValkeySentinel should throw when accessing workingDirectory before start")
    fun `sentinel throws on workingDirectory before start`() {
        val os = detectOperatingSystem()
        val sentinel = ValkeySentinelBuilder()
            .installationSupplier(os, createInstallationSupplier("#!/bin/sh\nsleep 1\n", os))
            .build()

        assertThrows(IllegalStateException::class.java) { sentinel.workingDirectory }
    }

    @Test
    @DisplayName("ValkeySentinel stop should be safe when not started")
    fun `sentinel stop is safe when not started`() {
        val os = detectOperatingSystem()
        val sentinel = ValkeySentinelBuilder()
            .installationSupplier(os, createInstallationSupplier("#!/bin/sh\nsleep 1\n", os))
            .build()

        sentinel.stop()
    }

    @Test
    @DisplayName("ValkeySentinel companion builder method should return a builder")
    fun `companion builder returns builder`() {
        assertThat(ValkeySentinel.builder()).isInstanceOf(ValkeySentinelBuilder::class.java)
    }
}
