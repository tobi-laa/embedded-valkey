package io.github.tobi.laa.embedded.valkey.process

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallation
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Tests for ValkeyProcess")
class ValkeyProcessTest {

    @Test
    @DisplayName("Process should start and stop successfully with readiness check")
    fun `starts and stops with readiness`() {
        val script = createScript(
            "#!/bin/sh\n" +
                "echo \"Ready to accept connections\"\n" +
                "while true; do sleep 1; done\n"
        )
        val installation = createInstallation(script)
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        process.start(awaitServerReady = true, maxWaitTimeSeconds = 2)
        process.start(awaitServerReady = true, maxWaitTimeSeconds = 2)
        process.stop(forcibly = true, maxWaitTimeSeconds = 1)
    }

    @Test
    @DisplayName("Sentinel process should start and match sentinel-specific readiness output")
    fun `starts sentinel and matches sentinel readiness`() {
        val script = createScript(
            "#!/bin/sh\n" +
                "echo \"Sentinel runid is 123\"\n" +
                "while true; do sleep 1; done\n"
        )
        val installation = createInstallation(script)
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build(), sentinel = true)

        process.start(awaitServerReady = true, maxWaitTimeSeconds = 2)
        process.stop(forcibly = true, maxWaitTimeSeconds = 1)
    }

    @Test
    @DisplayName("Starting should throw IOException when the process terminates prematurely")
    fun `start throws when process terminates early`() {
        val script = createScript("#!/bin/sh\nexit 0\n")
        val installation = createInstallation(script)
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        assertThrows(IOException::class.java) { process.start(awaitServerReady = true, maxWaitTimeSeconds = 1) }
    }

    @Test
    @DisplayName("Stopping a process that was never started should not throw")
    fun `stop is safe when never started`() {
        val script = createScript("#!/bin/sh\nexit 0\n")
        val installation = createInstallation(script)
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        assertDoesNotThrow { process.stop() }
    }

    @Test
    @DisplayName("Stopping should force termination when graceful shutdown times out")
    fun `stop forces termination on timeout`() {
        val script = createScript(
            "#!/bin/sh\n" +
                "trap '' TERM\n" +
                "while true; do sleep 1; done\n"
        )
        val installation = createInstallation(script)
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        process.start(awaitServerReady = false, maxWaitTimeSeconds = 1)
        process.stop(forcibly = false, maxWaitTimeSeconds = 0)
    }

    @Test
    @DisplayName("toString should return a representation without throwing")
    fun `toString reports args state`() {
        val script = createScript("#!/bin/sh\nexit 0\n")
        val installation = createInstallation(script)
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        assertDoesNotThrow { process.toString() }
    }

    @Test
    @DisplayName("Stopping with removeWorkingDirectory should delete the working directory")
    fun `stop removes working directory when requested`() {
        val script = createScript(
            "#!/bin/sh\n" +
                "echo \"Ready to accept connections\"\n" +
                "while true; do sleep 1; done\n"
        )
        val installation = createInstallation(script)
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
        val script = createScript(
            "#!/bin/sh\n" +
                "while true; do sleep 1; done\n"
        )
        val installation = createInstallation(script)
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
                    operatingSystem = OperatingSystem.LINUX_X86_64,
                    distributionType = DistributionType.VALKEY,
                    installationPath = installDir,
                    binaryPath = nonExistent
                ),
                config = ValkeyConfBuilder().build()
            )
        }
    }

    @Test
    @DisplayName("Constructor should reject a non-executable binary path")
    fun `constructor rejects non-executable binary`() {
        val installDir = Files.createTempDirectory("valkey-install")
        val notExecutable = Files.createTempFile(installDir, "valkey", ".sh")
        notExecutable.toFile().setExecutable(false)
        assertThrows(IllegalArgumentException::class.java) {
            ValkeyProcess(
                valkeyInstallation = ValkeyInstallation(
                    version = "9.0.2",
                    operatingSystem = OperatingSystem.LINUX_X86_64,
                    distributionType = DistributionType.VALKEY,
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
        val script = createScript("#!/bin/sh\nexit 0\n")
        val installation = createInstallation(script)
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
    @DisplayName("toString should include process PID after start")
    fun `toString includes pid after start`() {
        val script = createScript(
            "#!/bin/sh\n" +
                "echo \"Ready to accept connections\"\n" +
                "while true; do sleep 1; done\n"
        )
        val installation = createInstallation(script)
        val process = ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build())

        process.start(awaitServerReady = true, maxWaitTimeSeconds = 2)
        val str = process.toString()
        assertThat(str).contains("pid:")
        assertThat(str).doesNotContain("not started")
        process.stop(forcibly = true, maxWaitTimeSeconds = 1)
    }

    private fun createInstallation(binary: Path): ValkeyInstallation {
        val installDir = Files.createTempDirectory("valkey-install")
        return ValkeyInstallation(
            version = "9.0.2",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            distributionType = DistributionType.VALKEY,
            installationPath = installDir,
            binaryPath = binary
        )
    }

    private fun createScript(contents: String): Path {
        val script = Files.createTempFile("valkey-script", ".sh")
        Files.writeString(script, contents)
        script.toFile().setExecutable(true)
        return script
    }
}
