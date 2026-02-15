package io.github.tobi.laa.embedded.valkey.process

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallation
import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import io.github.tobi.laa.embedded.valkey.testing.ScriptBehavior
import io.github.tobi.laa.embedded.valkey.testing.createExecutableScript
import io.github.tobi.laa.embedded.valkey.testing.isWindows
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Tests for ValkeyProcess")
class ValkeyProcessTest {

    @Test
    @DisplayName("Process should start and stop successfully with readiness check")
    fun `starts and stops with readiness`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.ECHO_READY_AND_SLEEP))
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        process.start(awaitServerReady = true, maxWaitTimeSeconds = 2)
        process.start(awaitServerReady = true, maxWaitTimeSeconds = 2)
        process.stop(forcibly = true, maxWaitTimeSeconds = 1)
    }

    @Test
    @DisplayName("Sentinel process should start and match sentinel-specific readiness output")
    fun `starts sentinel and matches sentinel readiness`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.ECHO_SENTINEL_READY_AND_SLEEP))
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build(), sentinel = true)

        process.start(awaitServerReady = true, maxWaitTimeSeconds = 2)
        process.stop(forcibly = true, maxWaitTimeSeconds = 1)
    }

    @Test
    @DisplayName("Starting should throw IOException when the process terminates prematurely")
    fun `start throws when process terminates early`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.EXIT_IMMEDIATELY))
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        assertThrows(IOException::class.java) { process.start(awaitServerReady = true, maxWaitTimeSeconds = 1) }
    }

    @Test
    @DisplayName("Stopping a process that was never started should not throw")
    fun `stop is safe when never started`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.EXIT_IMMEDIATELY))
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        assertDoesNotThrow { process.stop() }
    }

    @Test
    @DisplayName("Stopping should force termination when graceful shutdown times out")
    fun `stop forces termination on timeout`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.IGNORE_TERM_AND_SLEEP))
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        process.start(awaitServerReady = false, maxWaitTimeSeconds = 1)
        process.stop(forcibly = false, maxWaitTimeSeconds = 0)
    }

    @Test
    @DisplayName("toString should return a representation without throwing")
    fun `toString reports args state`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.EXIT_IMMEDIATELY))
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        assertDoesNotThrow { process.toString() }
    }

    @Test
    @DisplayName("Stopping with removeWorkingDirectory should delete the working directory")
    fun `stop removes working directory when requested`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.ECHO_READY_AND_SLEEP))
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        process.start(awaitServerReady = true, maxWaitTimeSeconds = 2)
        val workDir = process.workingDirectory
        assertThat(workDir).exists()
        process.stop(forcibly = true, maxWaitTimeSeconds = 1, removeWorkingDirectory = true)
        assertThat(workDir).doesNotExist()
    }

    @Test
    @DisplayName("Starting should throw IOException when the process is alive but never becomes ready")
    fun `start throws when process never becomes ready`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.IGNORE_TERM_AND_SLEEP))
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        assertThrows(IOException::class.java) { process.start(awaitServerReady = true, maxWaitTimeSeconds = 1) }
    }

    @Test
    @DisplayName("Constructor should reject a non-existent binary path")
    fun `constructor rejects non-existent binary`() {
        val installDir = Files.createTempDirectory("valkey-install")
        val nonExistent = installDir.resolve("missing")
        assertThrows(IllegalArgumentException::class.java) {
            ValkeyProcess(
                valkeyInstallation = ValkeyInstallation(
                    version = "9.0.2",
                    operatingSystem = detectOperatingSystem(),
                    distributionType = if (isWindows()) DistributionType.MEMURAI else DistributionType.VALKEY,
                    installationPath = installDir,
                    binaryPath = nonExistent
                ),
                config = ValkeyConfBuilder().build()
            )
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    @DisplayName("Constructor should reject a non-executable binary path (Unix only, Windows has no executable permission bits)")
    fun `constructor rejects non-executable binary`() {
        val installDir = Files.createTempDirectory("valkey-install")
        val suffix = if (isWindows()) ".bat" else ".sh"
        val notExecutable = Files.createTempFile(installDir, "valkey", suffix)
        notExecutable.toFile().setExecutable(false)
        assertThrows(IllegalArgumentException::class.java) {
            ValkeyProcess(
                valkeyInstallation = ValkeyInstallation(
                    version = "9.0.2",
                    operatingSystem = detectOperatingSystem(),
                    distributionType = if (isWindows()) DistributionType.MEMURAI else DistributionType.VALKEY,
                    installationPath = installDir,
                    binaryPath = notExecutable
                ),
                config = ValkeyConfBuilder().build()
            )
        }
    }

    @Test
    @DisplayName("Property accessors should return the configured values")
    fun `property accessors return configured values`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.EXIT_IMMEDIATELY))
        val config = ValkeyConfBuilder().build()
        val process = ValkeyProcess(valkeyInstallation = installation, config = config)

        assertThat(process.valkeyInstallation).isEqualTo(installation)
        assertThat(process.workingDirectory).exists()
        assertThat(process.config).isEqualTo(config)
        assertThat(process.charset).isEqualTo(Charsets.UTF_8)
        assertThat(process.sentinel).isFalse()
        assertThat(process.active).isFalse()
        assertThat(process.ready).isFalse()
    }

    @Test
    @DisplayName("Constructor should reject a non-existent working directory")
    fun `constructor rejects non-existent working directory`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.EXIT_IMMEDIATELY))
        val nonExistentDir = Path.of(System.getProperty("java.io.tmpdir"), "non-existent-dir-" + System.nanoTime())
        assertThrows(IllegalArgumentException::class.java) {
            ValkeyProcess(
                valkeyInstallation = installation,
                workingDirectory = nonExistentDir,
                config = ValkeyConfBuilder().build()
            )
        }
    }

    @Test
    @DisplayName("Constructor should reject a working directory that is not a directory")
    fun `constructor rejects non-directory working directory`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.EXIT_IMMEDIATELY))
        val regularFile = Files.createTempFile("not-a-dir", ".txt")
        assertThrows(IllegalArgumentException::class.java) {
            ValkeyProcess(
                valkeyInstallation = installation,
                workingDirectory = regularFile,
                config = ValkeyConfBuilder().build()
            )
        }
    }

    @Test
    @DisplayName("Process should capture stderr output from the running process")
    fun `stderr output is captured`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.ECHO_READY_AND_STDERR_AND_SLEEP))
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        process.start(awaitServerReady = true, maxWaitTimeSeconds = 2)
        Thread.sleep(200) // give stderr thread time to read
        process.stop(forcibly = true, maxWaitTimeSeconds = 1)
    }

    @Test
    @DisplayName("toString should include process PID after start")
    fun `toString includes pid after start`() {
        val installation = createInstallation(createExecutableScript(ScriptBehavior.ECHO_READY_AND_SLEEP))
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        process.start(awaitServerReady = true, maxWaitTimeSeconds = 2)
        val processDescription = process.toString()
        assertThat(processDescription).contains("pid:")
        assertThat(processDescription).doesNotContain("not started")
        process.stop(forcibly = true, maxWaitTimeSeconds = 1)
    }

    private fun createInstallation(binary: Path): ValkeyInstallation {
        val installDir = Files.createTempDirectory("valkey-install")
        return ValkeyInstallation(
            version = "9.0.2",
            operatingSystem = detectOperatingSystem(),
            distributionType = if (isWindows()) DistributionType.MEMURAI else DistributionType.VALKEY,
            installationPath = installDir,
            binaryPath = binary
        )
    }
}
