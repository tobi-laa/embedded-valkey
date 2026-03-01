package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

@DisplayName("Tests for ValkeyPackage")
class ValkeyPackageTest {

    @Test
    @DisplayName("Package should require a non-blank version")
    fun `package requires non blank version`() {
        val path = Files.createTempFile("valkey", ".zip")

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyPackage(
                "",
                OperatingSystem.LINUX_X86_64,
                path = path,
                binaryPathWithinPackage = Paths.get("bin/valkey"),
                archiveType = ArchiveType.ZIP
            )
        }
    }

    @Test
    @DisplayName("Package should require an existing bundle file")
    fun `package requires existing bundle path`() {
        val missing = Files.createTempFile("valkey", ".zip")
        Files.deleteIfExists(missing)

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyPackage(
                "9.0.3",
                OperatingSystem.LINUX_X86_64,
                path = missing,
                binaryPathWithinPackage = Paths.get("bin/valkey"),
                archiveType = ArchiveType.ZIP
            )
        }
    }

    @Test
    @DisplayName("Package should require the binary path to be relative")
    fun `package requires relative binary path`() {
        val path = Files.createTempFile("valkey", ".zip")
        val absoluteBinary = path.toAbsolutePath()

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyPackage(
                "9.0.3",
                OperatingSystem.LINUX_X86_64,
                path = path,
                binaryPathWithinPackage = absoluteBinary,
                archiveType = ArchiveType.ZIP
            )
        }
    }
}
