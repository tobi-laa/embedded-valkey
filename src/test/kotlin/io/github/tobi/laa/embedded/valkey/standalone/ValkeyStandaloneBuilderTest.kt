package io.github.tobi.laa.embedded.valkey.standalone

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import io.github.tobi.laa.embedded.valkey.testing.ScriptBehavior
import io.github.tobi.laa.embedded.valkey.testing.createInstallationSupplier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Tests for ValkeyStandaloneBuilder")
class ValkeyStandaloneBuilderTest {

    @Test
    @DisplayName("Building should apply default port and bind addresses")
    fun `build applies default port and binds`() {
        val os = detectOperatingSystem()
        val builder = ValkeyStandaloneBuilder()
            .installationSupplier(os, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, os))

        val server = builder.build()

        assertThat(server.config.port()).isNotNull
        assertThat(server.config.binds()).contains("::1", "127.0.0.1")
    }

    @Test
    @DisplayName("Cloning should preserve custom installation suppliers and configuration")
    fun `clone preserves custom suppliers and configuration`() {
        val os = detectOperatingSystem()
        val builder = ValkeyStandaloneBuilder()
            .installationSupplier(os, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, os))
            .bind("0.0.0.0")
            .port(6390)
            .directive("maxmemory", "1mb")

        val cloned = builder.clone()
        val server = cloned.build()

        assertThat(server.config.port()).isEqualTo(6390)
        assertThat(server.config.binds()).contains("0.0.0.0")
        assertThat(server.config.directives("maxmemory")).isNotEmpty
    }

    @Test
    @DisplayName("Building should use replicaOf directive when configured")
    fun `build applies replicaOf`() {
        val os = detectOperatingSystem()
        val builder = ValkeyStandaloneBuilder()
            .installationSupplier(os, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, os))
            .replicaOf("localhost", 6379)

        val server = builder.build()

        assertThat(server.config.directives("replicaof")).isNotEmpty
    }

    @Test
    @DisplayName("ValkeyStandalone should report inactive when not started")
    fun `standalone reports inactive when not started`() {
        val os = detectOperatingSystem()
        val server = ValkeyStandaloneBuilder()
            .installationSupplier(os, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, os))
            .build()

        assertThat(server.active).isFalse()
    }

    @Test
    @DisplayName("ValkeyStandalone should throw when accessing workingDirectory before start")
    fun `standalone throws on workingDirectory before start`() {
        val os = detectOperatingSystem()
        val server = ValkeyStandaloneBuilder()
            .installationSupplier(os, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, os))
            .build()

        assertThrows(IllegalStateException::class.java) { server.workingDirectory }
    }

    @Test
    @DisplayName("ValkeyStandalone stop should be safe when not started")
    fun `standalone stop is safe when not started`() {
        val os = detectOperatingSystem()
        val server = ValkeyStandaloneBuilder()
            .installationSupplier(os, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, os))
            .build()

        server.stop()
    }

    @Test
    @DisplayName("ValkeyStandalone companion builder method should return a builder")
    fun `companion builder returns builder`() {
        assertThat(ValkeyStandalone.builder()).isInstanceOf(ValkeyStandaloneBuilder::class.java)
    }

    @Test
    @DisplayName("importConf(Path) should import configuration from a file")
    fun `importConf with path imports file`(@TempDir tempDir: Path) {
        val confFile = tempDir.resolve("test.conf")
        Files.writeString(confFile, "port 6399" + System.lineSeparator())

        val os = detectOperatingSystem()
        val server = ValkeyStandaloneBuilder()
            .installationSupplier(os, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, os))
            .importConf(confFile)
            .build()

        assertThat(server.config.port()).isEqualTo(6399)
    }

    @Test
    @DisplayName("importConf(ValkeyConf) should import configuration from ValkeyConf object")
    fun `importConf with ValkeyConf imports directives`() {
        val conf = ValkeyConfBuilder().port(6398).build()
        val os = detectOperatingSystem()
        val server = ValkeyStandaloneBuilder()
            .installationSupplier(os, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, os))
            .importConf(conf)
            .build()

        assertThat(server.config.port()).isEqualTo(6398)
    }
}
