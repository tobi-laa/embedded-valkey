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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ValkeyPackageExtractorTest {

    @Test
    fun `extracts zip archive to installation path`() {
        val archive = createZipArchive(mapOf("bin/valkey-server" to "binary".toByteArray(), "dir/" to ByteArray(0)))
        val installPath = Files.createTempDirectory("valkey-install")
        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.ZIP, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )

        val installation = extractor.installValkey()

        assertTrue(Files.exists(installation.binaryPath))
        assertTrue(Files.isDirectory(installPath.resolve("dir")))
    }

    @Test
    fun `extracts tar gz archive to installation path`() {
        val archive = createTarArchive(ArchiveType.TAR_GZ, mapOf("bin/valkey-server" to "binary".toByteArray()))
        val installPath = Files.createTempDirectory("valkey-install")
        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.TAR_GZ, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )

        val installation = extractor.installValkey()

        assertTrue(Files.exists(installation.binaryPath))
    }

    @Test
    fun `extracts tar bzip2 archive to installation path`() {
        val archive = createTarArchive(ArchiveType.TAR_BZ2, mapOf("bin/valkey-server" to "binary".toByteArray()))
        val installPath = Files.createTempDirectory("valkey-install")
        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.TAR_BZ2, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = true,
            ensureBinaryIsExecutable = false
        )

        val installation = extractor.installValkey()

        assertTrue(Files.exists(installation.binaryPath))
    }

    @Test
    fun `skips extraction when binary already exists`() {
        val installPath = Files.createTempDirectory("valkey-install")
        val binaryPath = installPath.resolve("bin/valkey-server")
        Files.createDirectories(binaryPath.parent)
        Files.write(binaryPath, "ready".toByteArray())
        val archive = Files.createTempFile("valkey", ".zip")

        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.ZIP, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = false,
            ensureBinaryIsExecutable = false
        )

        val installation = extractor.installValkey()

        assertEquals(binaryPath, installation.binaryPath)
    }

    @Test
    fun `throws when archive extraction attempts zip slip`() {
        val installPath = Files.createTempDirectory("valkey-install")
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
    fun `throws when binary is missing after extraction`() {
        val archive = createZipArchive(mapOf("bin/other" to "missing".toByteArray()))
        val installPath = Files.createTempDirectory("valkey-install")
        val extractor = ValkeyPackageExtractor(
            valkeyPackage = createPackage(archive, ArchiveType.ZIP, Paths.get("bin/valkey-server")),
            installationPath = installPath,
            alwaysExtract = true,
            ensureBinaryIsExecutable = true
        )

        assertThrows(IOException::class.java) { extractor.installValkey() }
    }

    @Test
    fun `resolves default temp installation path`() {
        val path = resolveDefaultTempInstallationPath(
            DistributionType.VALKEY,
            "9.0.2",
            OperatingSystem.LINUX_X86_64
        )

        assertTrue(path.toString().contains("valkey-9.0.2-linux_x86_64"))
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
        val file = Files.createTempFile("valkey", ".zip")
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
        val file = Files.createTempFile("valkey", suffix)
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
