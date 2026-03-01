package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallation
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@DisplayName("Tests for ValkeyPackageSupplier")
class ValkeyPackageSupplierTest {

    @field:TempDir
    private lateinit var tempDir: Path

    private var givenBinaryContent: ByteArray? = null
    private var givenPackage: Path? = null
    private var givenInstallationPath: Path? = null
    private var givenPackageSupplier: ValkeyPackageSupplier? = null
    private var extractAndInstall: ThrowableAssert.ThrowingCallable? = null
    private var actualValkeyInstallation: ValkeyInstallation? = null

    @BeforeEach
    fun reset() {
        givenBinaryContent = null
        givenPackage = null
        givenInstallationPath = null
        givenPackageSupplier = null
        extractAndInstall = null
        actualValkeyInstallation = null
    }

    @Test
    @DisplayName("thenExtract should install the package to a default temp path and produce a valid installation")
    fun `thenExtract installs package using default temp path`() {
        givenPseudoValkeyPackageSupplier()
        whenPackageExtractedAndInstalled()
        thenNoErrorShouldOccur()
        thenInstallationPathShouldExist()
        thenBinaryShouldContainExpectedContent()
    }

    @Test
    @DisplayName("thenExtract with alwaysExtract=true should install the package to a default temp path and produce a valid installation")
    fun `thenExtract with alwaysExtract installs package using default temp path`() {
        givenPseudoValkeyPackageSupplier()
        whenPackageForceExtractedAndInstalled()
        thenNoErrorShouldOccur()
        thenInstallationPathShouldExist()
        thenBinaryShouldContainExpectedContent()
    }

    @Test
    @DisplayName("thenExtract with ensureBinaryIsExecutable parameter should use defaults for other params")
    fun `thenExtract with ensureBinaryIsExecutable installs package using default temp path`() {
        givenPseudoValkeyPackageSupplier()
        whenPackageExtractedInstalledAndMadeExecutable()
        thenNoErrorShouldOccur()
        thenInstallationPathShouldExist()
        thenBinaryShouldContainExpectedContent()
    }

    @Test
    @DisplayName("thenExtract with ensureBinaryIsExecutable parameter should use defaults for other params")
    fun `thenExtract with custom installation folder installs package using default temp path`() {
        givenPseudoValkeyPackageSupplier()
        whenPackageExtractedAndInstalledToCustomFolder()
        thenNoErrorShouldOccur()
        thenInstallationPathShouldExistUnderneathCustomFolder()
        thenBinaryShouldContainExpectedContent()
    }

    private fun givenPseudoValkeyPackageSupplier() {
        givenBinaryContent = "binary".toByteArray()
        givenPackage = Files.createTempFile(tempDir, "valkey-package", ".zip")
        ZipOutputStream(Files.newOutputStream(givenPackage!!)).use { zip ->
            zip.putNextEntry(ZipEntry("valkey/bin/valkey-server"))
            zip.write(givenBinaryContent!!)
            zip.closeEntry()
        }
        givenPackageSupplier = object : ValkeyPackageSupplier {
            override fun retrievePackage(): ValkeyPackage {
                return ValkeyPackage(
                    version = "9.0.3",
                    operatingSystem = OperatingSystem.LINUX_X86_64,
                    distributionType = DistributionType.VALKEY,
                    path = givenPackage!!,
                    binaryPathWithinPackage = Path.of("valkey", "bin", "valkey-server"),
                    archiveType = ArchiveType.ZIP
                )
            }
        }
    }

    private fun whenPackageExtractedAndInstalled() {
        extractAndInstall = { actualValkeyInstallation = givenPackageSupplier!!.thenExtract().installValkey() }
    }

    private fun whenPackageForceExtractedAndInstalled() {
        extractAndInstall =
            { actualValkeyInstallation = givenPackageSupplier!!.thenExtract(alwaysExtract = true).installValkey() }
    }

    private fun whenPackageExtractedInstalledAndMadeExecutable() {
        extractAndInstall = {
            actualValkeyInstallation =
                givenPackageSupplier!!.thenExtract(ensureBinaryIsExecutable = true).installValkey()
        }
    }

    private fun whenPackageExtractedAndInstalledToCustomFolder() {
        givenInstallationPath = Files.createTempDirectory(tempDir, null)
        extractAndInstall =
            {
                actualValkeyInstallation =
                    givenPackageSupplier!!.thenExtract(installationPath = givenInstallationPath).installValkey()
            }
    }

    private fun thenNoErrorShouldOccur() {
        assertThatCode(extractAndInstall).doesNotThrowAnyException()
    }

    private fun thenInstallationPathShouldExist() {
        assertThat(actualValkeyInstallation!!.installationPath).exists()
    }

    private fun thenInstallationPathShouldExistUnderneathCustomFolder() {
        assertThat(actualValkeyInstallation!!.installationPath).startsWith(givenInstallationPath!!)
    }

    private fun thenBinaryShouldContainExpectedContent() {
        assertThat(actualValkeyInstallation!!.binaryPath).hasBinaryContent(givenBinaryContent)
    }
}
