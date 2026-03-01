package io.github.tobi.laa.embedded.valkey.installation

import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import io.github.tobi.laa.embedded.valkey.valkeypackage.ArchiveType
import io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackage
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.*

@DisplayName("Tests for ValkeyPackageExtractor")
class ValkeyPackageExtractorTest {

    companion object {
        private const val BINARY_RELATIVE_PATH = "bin/valkey-server"
    }

    @TempDir
    private lateinit var tempDir: Path

    private var givenExtractor: ValkeyPackageExtractor? = null
    private var givenArchiveStream: ArchiveInputStream<ArchiveEntry>? = null
    private var givenBinaryPath: Path? = null
    private var givenMockedFileConstruction: MockedConstruction<File>? = null
    private var performAction: ThrowableAssert.ThrowingCallable? = null
    private var installation: ValkeyInstallation? = null
    private var resolvedPath: Path? = null

    @BeforeEach
    fun reset() {
        givenExtractor = null
        givenArchiveStream = null
        givenBinaryPath = null
        givenMockedFileConstruction = null
        performAction = null
        installation = null
        resolvedPath = null
    }

    @AfterEach
    fun cleanUpMocks() {
        givenMockedFileConstruction?.close()
    }

    @Test
    @DisplayName("ZIP archive should be extracted to the installation path, creating both file entries and directory entries")
    fun `extracts zip archive to installation path`() {
        givenZipExtractor(mapOf(BINARY_RELATIVE_PATH to "binary".toByteArray(), "dir/" to ByteArray(0)))
        whenInstallValkeyIsCalled()
        thenNoErrorOccurs()
        thenBinaryExists()
        thenDirectoryExists("dir")
    }

    @Test
    @DisplayName("TAR.GZ archive should be extracted to the installation path")
    fun `extracts tar gz archive to installation path`() {
        givenTarGzExtractor(mapOf(BINARY_RELATIVE_PATH to "binary".toByteArray()))
        whenInstallValkeyIsCalled()
        thenNoErrorOccurs()
        thenBinaryExists()
    }

    @Test
    @DisplayName("TAR.BZ2 archive should be extracted to the installation path")
    fun `extracts tar bzip2 archive to installation path`() {
        givenTarBz2Extractor(mapOf(BINARY_RELATIVE_PATH to "binary".toByteArray()))
        whenInstallValkeyIsCalled()
        thenNoErrorOccurs()
        thenBinaryExists()
    }

    @Test
    @DisplayName("Extraction should be skipped when the binary already exists at the installation path")
    fun `skips extraction when binary already exists`() {
        givenExtractorWithPreexistingBinary()
        whenInstallValkeyIsCalled()
        thenNoErrorOccurs()
        thenBinaryIsPreExistingBinary()
    }

    @Test
    @DisplayName("An IOException with a zip slip message should be thrown when an archive entry would be extracted outside the installation path")
    fun `throws when archive extraction attempts zip slip`() {
        givenExtractorWithZipSlipArchiveStream()
        whenExtractArchiveIsCalled()
        thenIOExceptionIsThrownContaining("Zip (or archive) slip detected")
    }

    @Test
    @DisplayName("An IOException with a 'does not exist' message should be thrown when the binary is missing after extraction")
    fun `throws when binary is missing after extraction`() {
        givenExtractorWithMissingBinary()
        whenInstallValkeyIsCalled()
        thenIOExceptionIsThrownContaining("does not exist after extraction")
    }

    @Test
    @DisplayName("An IOException with a 'Failed to make executable' message should be thrown when the binary could not be made executable")
    fun `throws when binary could not be made executable`() {
        givenExtractorWithNonExecutableBinary()
        whenInstallValkeyIsCalled()
        thenIOExceptionIsThrownContaining("Failed to make")
    }

    @Test
    @DisplayName("Default temp installation path should follow the naming convention '<distribution>-<version>-<os>'")
    fun `resolves default temp installation path`() {
        whenDefaultInstallationPathIsResolved()
        thenResolvedPathContains("valkey-9.0.2-linux_x86_64")
    }

    private fun givenZipExtractor(entries: Map<String, ByteArray>) {
        val archive = createZipArchive(entries)
        givenExtractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.ZIP, Paths.get(BINARY_RELATIVE_PATH)),
            installationPath = createInstallDir("zip-install"),
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )
    }

    private fun givenTarGzExtractor(entries: Map<String, ByteArray>) {
        val archive = createTarArchive(ArchiveType.TAR_GZ, entries)
        givenExtractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.TAR_GZ, Paths.get(BINARY_RELATIVE_PATH)),
            installationPath = createInstallDir("targz-install"),
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )
    }

    private fun givenTarBz2Extractor(entries: Map<String, ByteArray>) {
        val archive = createTarArchive(ArchiveType.TAR_BZ2, entries)
        givenExtractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.TAR_BZ2, Paths.get(BINARY_RELATIVE_PATH)),
            installationPath = createInstallDir("tarbz2-install"),
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )
    }

    private fun givenExtractorWithPreexistingBinary() {
        val installPath = createInstallDir("skip-install")
        val binaryPath = installPath.resolve(BINARY_RELATIVE_PATH)
        Files.createDirectories(binaryPath.parent)
        Files.write(binaryPath, "ready".toByteArray())
        givenBinaryPath = binaryPath
        givenExtractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(
                Files.createFile(tempDir.resolve("dummy.zip")),
                ArchiveType.ZIP,
                Paths.get(BINARY_RELATIVE_PATH)
            ),
            installationPath = installPath,
            alwaysExtract = false,
            ensureBinaryIsExecutable = false
        )
    }

    private fun givenExtractorWithZipSlipArchiveStream() {
        givenExtractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(
                Files.createTempFile(tempDir, "dummy", ".zip"),
                ArchiveType.ZIP,
                Paths.get(BINARY_RELATIVE_PATH)
            ),
            installationPath = createInstallDir("zipslip-install"),
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )
        givenArchiveStream = SingleEntryArchiveInputStream(ZipArchiveEntry("/etc/passwd"))
    }

    private fun givenExtractorWithMissingBinary() {
        val archive = createZipArchive(mapOf("bin/other" to "missing".toByteArray()))
        givenExtractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.ZIP, Paths.get(BINARY_RELATIVE_PATH)),
            installationPath = createInstallDir("missing-install"),
            alwaysExtract = true,
            ensureBinaryIsExecutable = true
        )
    }

    private fun givenExtractorWithNonExecutableBinary() {
        givenMockedFileConstruction = Mockito.mockConstruction(File::class.java) { mock, _ ->
            Mockito.`when`(mock.setExecutable(true)).thenReturn(false)
        }
        val archive = createZipArchive(mapOf(BINARY_RELATIVE_PATH to "binary".toByteArray()))
        givenExtractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.ZIP, Paths.get(BINARY_RELATIVE_PATH)),
            installationPath = createInstallDir("non-exec-install"),
            alwaysExtract = true,
            ensureBinaryIsExecutable = true
        )
    }

    private fun whenInstallValkeyIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            installation = givenExtractor!!.installValkey()
        }
    }

    private fun whenExtractArchiveIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            givenExtractor!!.extractArchive(givenArchiveStream!!, givenExtractor!!.installationPath)
        }
    }

    private fun whenDefaultInstallationPathIsResolved() {
        performAction = ThrowableAssert.ThrowingCallable {
            resolvedPath = resolveDefaultTempInstallationPath(
                DistributionType.VALKEY,
                "9.0.2",
                OperatingSystem.LINUX_X86_64
            )
        }
    }

    private fun thenNoErrorOccurs() {
        assertThatCode(performAction!!).doesNotThrowAnyException()
    }

    private fun thenBinaryExists() {
        assertThat(installation!!.binaryPath).exists()
    }

    private fun thenDirectoryExists(name: String) {
        assertThat(givenExtractor!!.installationPath.resolve(name)).isDirectory()
    }

    private fun thenBinaryIsPreExistingBinary() {
        assertThat(installation!!.binaryPath).isEqualTo(givenBinaryPath)
    }

    private fun thenIOExceptionIsThrownContaining(message: String) {
        assertThatCode(performAction!!).isExactlyInstanceOf(IOException::class.java)
            .hasMessageContaining(message)
    }

    private fun thenResolvedPathContains(expected: String) {
        assertThatCode(performAction!!).doesNotThrowAnyException()
        assertThat(resolvedPath!!.toString()).contains(expected)
    }

    private fun createInstallDir(name: String): Path {
        val path = tempDir.resolve(name)
        Files.createDirectories(path)
        return path
    }

    private fun createPackage(archive: Path, archiveType: ArchiveType, binaryPath: Path): ValkeyPackage {
        return ValkeyPackage(
            version = "9.0.2",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            path = archive,
            binaryPathWithinPackage = binaryPath,
            archiveType = archiveType
        )
    }

    private fun createZipArchive(entries: Map<String, ByteArray>): Path {
        val file = tempDir.resolve("archive-${System.nanoTime()}.zip")
        ZipArchiveOutputStream(Files.newOutputStream(file)).use { output ->
            for ((name, content) in entries) {
                val entry = ZipArchiveEntry(name)
                if (!name.endsWith("/")) {
                    entry.size = content.size.toLong()
                }
                output.putArchiveEntry(entry)
                if (!name.endsWith("/")) {
                    output.write(content)
                }
                output.closeArchiveEntry()
            }
        }
        return file
    }

    private fun createTarArchive(type: ArchiveType, entries: Map<String, ByteArray>): Path {
        val suffix = if (type == ArchiveType.TAR_GZ) ".tar.gz" else ".tar.bz2"
        val file = tempDir.resolve("archive-${System.nanoTime()}$suffix")
        val baseStream = Files.newOutputStream(file)
        val compressed = if (type == ArchiveType.TAR_GZ) {
            GzipCompressorOutputStream(baseStream)
        } else {
            BZip2CompressorOutputStream(baseStream)
        }
        TarArchiveOutputStream(compressed).use { output ->
            for ((name, content) in entries) {
                val entry = TarArchiveEntry(name)
                entry.size = content.size.toLong()
                output.putArchiveEntry(entry)
                output.write(content)
                output.closeArchiveEntry()
            }
        }
        return file
    }

    private class SingleEntryArchiveInputStream(private val entry: ArchiveEntry) : ArchiveInputStream<ArchiveEntry>() {
        private var delivered = false

        override fun getNextEntry(): ArchiveEntry? {
            if (delivered) return null
            delivered = true
            return entry
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int = -1
    }
}
