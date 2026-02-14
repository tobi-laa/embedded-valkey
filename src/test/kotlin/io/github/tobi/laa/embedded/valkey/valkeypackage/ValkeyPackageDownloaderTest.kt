package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

class ValkeyPackageDownloaderTest {

    @Test
    fun `downloads package with checksum verification and copies from cache`() {
        val source = Files.createTempFile("valkey-source", ".zip")
        val content = "payload".toByteArray()
        Files.write(source, content)
        val cacheFile = Files.createTempFile("valkey-cache", ".zip")
        Files.deleteIfExists(cacheFile)
        val downloadLocation = Files.createTempDirectory("valkey-download").resolve("valkey.zip")
        val checksum = sha256(content)

        val downloader = ValkeyPackageDownloader(
            valkeyVersion = "9.0.2",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            binaryPathWithinPackage = Paths.get("bin/valkey-server"),
            archiveType = ArchiveType.ZIP,
            downloadUri = source.toUri(),
            cacheFileLocation = cacheFile,
            downloadLocation = downloadLocation,
            sha256FileChecksum = checksum,
            verifyFileChecksum = true,
            distributionType = DistributionType.VALKEY
        )

        val packageResult = downloader.retrievePackage()

        assertTrue(Files.exists(cacheFile))
        assertTrue(Files.exists(downloadLocation))
        assertEquals(downloadLocation, packageResult.path)
        assertEquals(checksum, sha256(Files.readAllBytes(downloadLocation)))
    }

    @Test
    fun `uses cached file without downloading`() {
        val cacheFile = Files.createTempFile("valkey-cache", ".zip")
        Files.write(cacheFile, "cached".toByteArray())
        val downloadLocation = Files.createTempDirectory("valkey-download").resolve("valkey.zip")

        val downloader = ValkeyPackageDownloader(
            valkeyVersion = "9.0.2",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            binaryPathWithinPackage = Paths.get("bin/valkey-server"),
            archiveType = ArchiveType.ZIP,
            downloadUri = URI("file:///does-not-exist"),
            cacheFileLocation = cacheFile,
            downloadLocation = downloadLocation,
            cacheDownload = true
        )

        val packageResult = downloader.retrievePackage()

        assertEquals(downloadLocation, packageResult.path)
        assertTrue(Files.exists(downloadLocation))
    }

    @Test
    fun `download without cache writes to download location`() {
        val source = Files.createTempFile("valkey-source", ".zip")
        Files.write(source, "payload".toByteArray())
        val downloadLocation = Files.createTempDirectory("valkey-download").resolve("valkey.zip")

        val downloader = ValkeyPackageDownloader(
            valkeyVersion = "9.0.2",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            binaryPathWithinPackage = Paths.get("bin/valkey-server"),
            archiveType = ArchiveType.ZIP,
            downloadUri = source.toUri(),
            cacheDownload = false,
            downloadLocation = downloadLocation
        )

        val packageResult = downloader.retrievePackage()

        assertEquals(downloadLocation, packageResult.path)
        assertTrue(Files.exists(downloadLocation))
    }

    @Test
    fun `checksum mismatch throws exception`() {
        val source = Files.createTempFile("valkey-source", ".zip")
        Files.write(source, "payload".toByteArray())

        val downloader = ValkeyPackageDownloader(
            valkeyVersion = "9.0.2",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            binaryPathWithinPackage = Paths.get("bin/valkey-server"),
            archiveType = ArchiveType.ZIP,
            downloadUri = source.toUri(),
            sha256FileChecksum = "deadbeef",
            verifyFileChecksum = true
        )

        assertThrows(FileChecksumMismatchException::class.java) { downloader.retrievePackage() }
    }

    @Test
    fun `constructor validation requires version and checksum`() {
        assertThrows(IllegalArgumentException::class.java) {
            ValkeyPackageDownloader(
                valkeyVersion = " ",
                operatingSystem = OperatingSystem.LINUX_X86_64,
                binaryPathWithinPackage = Paths.get("bin/valkey-server"),
                archiveType = ArchiveType.ZIP,
                downloadUri = URI("file:///tmp/valkey.zip")
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ValkeyPackageDownloader(
                valkeyVersion = "9.0.2",
                operatingSystem = OperatingSystem.LINUX_X86_64,
                binaryPathWithinPackage = Paths.get("bin/valkey-server"),
                archiveType = ArchiveType.ZIP,
                downloadUri = URI("file:///tmp/valkey.zip"),
                sha256FileChecksum = null,
                verifyFileChecksum = true
            )
        }
    }

    @Test
    fun `human readable byte count covers all branches`() {
        val method = Class.forName("io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackageDownloaderKt")
            .getDeclaredMethod("humanReadableByteCount", Long::class.javaPrimitiveType)
        method.isAccessible = true

        val cases = listOf(
            -1L,
            10L,
            1024L,
            1_048_576L,
            1_073_741_824L,
            1_099_511_627_776L,
            1_125_844_931_261_235L,
            1_125_844_931_261_236L
        )

        for (bytes in cases) {
            val result = method.invoke(null, bytes) as String
            assertTrue(result.isNotBlank())
        }
    }

    @Test
    fun `default cache path builder uses expected layout`() {
        val method = Class.forName("io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackageDownloaderKt")
            .getDeclaredMethod("resolveDefaultTempFilePath", String::class.java, OperatingSystem::class.java, ArchiveType::class.java)
        method.isAccessible = true

        val path = method.invoke(null, "9.0.2", OperatingSystem.LINUX_X86_64, ArchiveType.ZIP) as Path
        val asString = path.toString()

        assertTrue(asString.contains("valkey-9.0.2-linux_x86_64"))
        assertTrue(asString.endsWith(".zip"))
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
