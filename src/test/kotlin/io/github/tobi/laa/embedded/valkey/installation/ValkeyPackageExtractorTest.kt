package io.github.tobi.laa.embedded.valkey.installation

import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import io.github.tobi.laa.embedded.valkey.valkeypackage.ArchiveType
import io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackage
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@DisplayName("Tests for ValkeyPackageExtractor")
class ValkeyPackageExtractorTest {

    @TempDir
    private lateinit var tempDir: Path

    @Test
    @DisplayName("ZIP archive should be extracted to the installation path")
    fun `extracts zip archive to installation path`() {
        val archive = createZipArchive(mapOf("bin/valkey-server" to "binary".toByteArray(), "dir/" to ByteArray(0)))
        val installPath = tempDir.resolve("zip-install")
        Files.createDirectories(installPath)
        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.ZIP, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )

        val installation = extractor.installValkey()

        assertThat(installation.binaryPath).exists()
        assertThat(installPath.resolve("dir")).isDirectory()
    }

    @Test
    @DisplayName("TAR.GZ archive should be extracted to the installation path")
    fun `extracts tar gz archive to installation path`() {
        val archive = createTarArchive(ArchiveType.TAR_GZ, mapOf("bin/valkey-server" to "binary".toByteArray()))
        val installPath = tempDir.resolve("targz-install")
        Files.createDirectories(installPath)
        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.TAR_GZ, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )

        val installation = extractor.installValkey()

        assertThat(installation.binaryPath).exists()
    }

    @Test
    @DisplayName("TAR.BZ2 archive should be extracted to the installation path")
    fun `extracts tar bzip2 archive to installation path`() {
        val archive = createTarArchive(ArchiveType.TAR_BZ2, mapOf("bin/valkey-server" to "binary".toByteArray()))
        val installPath = tempDir.resolve("tarbz2-install")
        Files.createDirectories(installPath)
        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.TAR_BZ2, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )

        val installation = extractor.installValkey()

        assertThat(installation.binaryPath).exists()
    }

    @Test
    @DisplayName("Extraction should be skipped when the binary already exists")
    fun `skips extraction when binary already exists`() {
        val installPath = tempDir.resolve("skip-install")
        val binaryPath = installPath.resolve("bin/valkey-server")
        Files.createDirectories(binaryPath.parent)
        Files.write(binaryPath, "ready".toByteArray())
        val archive = tempDir.resolve("dummy.zip")
        Files.createFile(archive)

        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.ZIP, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = false,
            ensureBinaryIsExecutable = false
        )

        val installation = extractor.installValkey()

        assertThat(installation.binaryPath).isEqualTo(binaryPath)
    }

    @Test
    @DisplayName("Zip slip attack should be detected and rejected")
    fun `throws when archive extraction attempts zip slip`() {
        val installPath = tempDir.resolve("zipslip-install")
        Files.createDirectories(installPath)
        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(createZipArchive(mapOf("bin/valkey-server" to "ok".toByteArray())), ArchiveType.ZIP, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )

        val entry = ZipArchiveEntry("/etc/passwd")
        val stream = SingleEntryArchiveInputStream(entry)
        assertThrows(IOException::class.java) { extractor.extractArchive(stream, installPath) }
    }

    @Test
    @DisplayName("Missing binary after extraction should cause an error")
    fun `throws when binary is missing after extraction`() {
        val archive = createZipArchive(mapOf("bin/other" to "missing".toByteArray()))
        val installPath = tempDir.resolve("missing-install")
        Files.createDirectories(installPath)
        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.ZIP, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = true,
            ensureBinaryIsExecutable = true
        )

        assertThrows(IOException::class.java) { extractor.installValkey() }
    }

    @Test
    @DisplayName("Default temp installation path should follow the expected naming convention")
    fun `resolves default temp installation path`() {
        val path = resolveDefaultTempInstallationPath(
            DistributionType.VALKEY,
            "9.0.2",
            OperatingSystem.LINUX_X86_64
        )

        assertThat(path.toString()).contains("valkey-9.0.2-linux_x86_64")
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
