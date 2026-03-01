package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Path

@DisplayName("Tests for ClasspathPackageSupplier")
class ClasspathPackageSupplierTest {

    @Test
    @DisplayName("Should reject a blank version string")
    fun `rejects blank version`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClasspathPackageSupplier(
                classpathResource = "/valkey-packages/test.tar.gz",
                valkeyVersion = "   ",
                operatingSystem = OperatingSystem.LINUX_X86_64,
                binaryPathWithinPackage = Path.of("bin/valkey-server"),
                archiveType = ArchiveType.TAR_GZ
            )
        }
    }

    @Test
    @DisplayName("Should throw IOException when classpath resource does not exist")
    fun `throws when classpath resource missing`() {
        val supplier = ClasspathPackageSupplier(
            classpathResource = "/nonexistent/package.tar.gz",
            valkeyVersion = "9.0.3",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            binaryPathWithinPackage = Path.of("bin/valkey-server"),
            archiveType = ArchiveType.TAR_GZ
        )

        assertThrows(IOException::class.java) { supplier.retrievePackage() }
    }
}
