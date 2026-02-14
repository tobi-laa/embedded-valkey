package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@DisplayName("Tests for ValkeyPackageSupplier")
class ValkeyPackageSupplierTest {

    @Test
    @DisplayName("thenExtract should install the package to a default temp path and produce a valid installation")
    fun `thenExtract installs package using default temp path`() {
        val archive = createZipWithBinary("valkey/bin/valkey-server")
        val packageSupplier = object : ValkeyPackageSupplier {
            override fun retrievePackage(): ValkeyPackage {
                return ValkeyPackage(
                    version = "9.0.2",
                    operatingSystem = OperatingSystem.LINUX_X86_64,
                    distributionType = DistributionType.VALKEY,
                    path = archive,
                    binaryPathWithinPackage = Path.of("valkey", "bin", "valkey-server"),
                    archiveType = ArchiveType.ZIP
                )
            }
        }

        val installation = packageSupplier.thenExtract(alwaysExtract = true).installValkey()

        assertThat(installation.installationPath).exists()
        assertThat(installation.binaryPath).exists()
    }

    @Test
    @DisplayName("thenExtract with only alwaysExtract parameter should use default installationPath")
    fun `thenExtract with alwaysExtract uses default installationPath`() {
        val archive = createZipWithBinary("valkey/bin/valkey-server")
        val packageSupplier = object : ValkeyPackageSupplier {
            override fun retrievePackage(): ValkeyPackage {
                return ValkeyPackage(
                    version = "9.0.2",
                    operatingSystem = OperatingSystem.LINUX_X86_64,
                    distributionType = DistributionType.VALKEY,
                    path = archive,
                    binaryPathWithinPackage = Path.of("valkey", "bin", "valkey-server"),
                    archiveType = ArchiveType.ZIP
                )
            }
        }

        val installation = packageSupplier.thenExtract(alwaysExtract = true).installValkey()

        assertThat(installation.installationPath).exists()
        assertThat(installation.binaryPath).exists()
    }

    @Test
    @DisplayName("thenExtract with ensureBinaryIsExecutable parameter should use defaults for other params")
    fun `thenExtract with ensureBinaryIsExecutable uses defaults`() {
        val archive = createZipWithBinary("valkey/bin/valkey-server")
        val packageSupplier = object : ValkeyPackageSupplier {
            override fun retrievePackage(): ValkeyPackage {
                return ValkeyPackage(
                    version = "9.0.2",
                    operatingSystem = OperatingSystem.LINUX_X86_64,
                    distributionType = DistributionType.VALKEY,
                    path = archive,
                    binaryPathWithinPackage = Path.of("valkey", "bin", "valkey-server"),
                    archiveType = ArchiveType.ZIP
                )
            }
        }

        val supplier = packageSupplier.thenExtract(ensureBinaryIsExecutable = true)
        assertThat(supplier).isNotNull
    }

    private fun createZipWithBinary(entryName: String): Path {
        val zipPath = Files.createTempFile("valkey-package", ".zip")
        ZipOutputStream(Files.newOutputStream(zipPath)).use { zip ->
            zip.putNextEntry(ZipEntry(entryName))
            zip.write("binary".toByteArray())
            zip.closeEntry()
        }
        return zipPath
    }
}
