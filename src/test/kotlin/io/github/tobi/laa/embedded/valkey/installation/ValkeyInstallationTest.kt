package io.github.tobi.laa.embedded.valkey.installation

import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files

@DisplayName("Tests for ValkeyInstallation")
class ValkeyInstallationTest {

    @Test
    @DisplayName("Installation should require a non-blank version")
    fun `installation requires non blank version`() {
        val dir = Files.createTempDirectory("valkey-install")
        val binary = Files.createTempFile(dir, "valkey", "bin")

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyInstallation("", OperatingSystem.LINUX_X86_64, installationPath = dir, binaryPath = binary)
        }
    }

    @Test
    @DisplayName("Installation should require an existing directory")
    fun `installation requires existing directory`() {
        val missingDir = Files.createTempDirectory("valkey-install").resolve("missing")
        val binary = Files.createTempFile("valkey", "bin")

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyInstallation("9.0.2", OperatingSystem.LINUX_X86_64, installationPath = missingDir, binaryPath = binary)
        }
    }

    @Test
    @DisplayName("Installation path must be a directory, not a file")
    fun `installation requires installation path to be a directory`() {
        val filePath = Files.createTempFile("valkey-install", "file")
        val binary = Files.createTempFile("valkey", "bin")

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyInstallation("9.0.2", OperatingSystem.LINUX_X86_64, installationPath = filePath, binaryPath = binary)
        }
    }

    @Test
    @DisplayName("Installation should require the binary to exist")
    fun `installation requires binary to exist`() {
        val dir = Files.createTempDirectory("valkey-install")
        val missingBinary = dir.resolve("valkey-server")

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyInstallation("9.0.2", OperatingSystem.LINUX_X86_64, installationPath = dir, binaryPath = missingBinary)
        }
    }
}
