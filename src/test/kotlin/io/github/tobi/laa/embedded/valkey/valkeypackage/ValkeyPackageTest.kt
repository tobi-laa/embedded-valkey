package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class ValkeyPackageTest {

    @Test
    fun `package requires non blank version`() {
        val path = Files.createTempFile("valkey", ".zip")

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyPackage("", OperatingSystem.LINUX_X86_64, path = path, binaryPathWithinPackage = Paths.get("bin/valkey"), archiveType = ArchiveType.ZIP)
        }
    }

    @Test
    fun `package requires existing bundle path`() {
        val missing = Files.createTempFile("valkey", ".zip")
        Files.deleteIfExists(missing)

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyPackage("9.0.2", OperatingSystem.LINUX_X86_64, path = missing, binaryPathWithinPackage = Paths.get("bin/valkey"), archiveType = ArchiveType.ZIP)
        }
    }

    @Test
    fun `package requires relative binary path`() {
        val path = Files.createTempFile("valkey", ".zip")
        val absoluteBinary = path.toAbsolutePath()

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyPackage("9.0.2", OperatingSystem.LINUX_X86_64, path = path, binaryPathWithinPackage = absoluteBinary, archiveType = ArchiveType.ZIP)
        }
    }
}
