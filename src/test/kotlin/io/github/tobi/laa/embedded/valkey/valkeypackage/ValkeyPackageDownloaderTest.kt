package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

@DisplayName("Tests for ValkeyPackageDownloader")
class ValkeyPackageDownloaderTest {

    @TempDir
    private lateinit var tempDir: Path

    @Test
    @DisplayName("Downloaded package should be cached and verified against its checksum")
    fun `downloads package with checksum verification and copies from cache`() {
        val source = tempDir.resolve("valkey-source.zip")
        val content = "payload".toByteArray()
        Files.write(source, content)
        val cacheFile = tempDir.resolve("cache").resolve("valkey-cache.zip")
        Files.createDirectories(cacheFile.parent)
        val downloadLocation = tempDir.resolve("download").resolve("valkey.zip")
        Files.createDirectories(downloadLocation.parent)
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

        assertThat(cacheFile).exists()
        assertThat(downloadLocation).exists()
        assertThat(packageResult.path).isEqualTo(downloadLocation)
        assertThat(sha256(Files.readAllBytes(downloadLocation))).isEqualTo(checksum)
    }

    @Test
    @DisplayName("Cached file should be used without re-downloading")
    fun `uses cached file without downloading`() {
        val cacheFile = tempDir.resolve("cache").resolve("valkey-cache.zip")
        Files.createDirectories(cacheFile.parent)
        Files.write(cacheFile, "cached".toByteArray())
        val downloadLocation = tempDir.resolve("download").resolve("valkey.zip")
        Files.createDirectories(downloadLocation.parent)

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

        assertThat(packageResult.path).isEqualTo(downloadLocation)
        assertThat(downloadLocation).exists()
    }

    @Test
    @DisplayName("Download without caching should write directly to the download location")
    fun `download without cache writes to download location`() {
        val source = tempDir.resolve("valkey-source.zip")
        Files.write(source, "payload".toByteArray())
        val downloadLocation = tempDir.resolve("download").resolve("valkey.zip")
        Files.createDirectories(downloadLocation.parent)

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

        assertThat(packageResult.path).isEqualTo(downloadLocation)
        assertThat(downloadLocation).exists()
    }

    @Test
    @DisplayName("Checksum mismatch should throw FileChecksumMismatchException")
    fun `checksum mismatch throws exception`() {
        val source = tempDir.resolve("valkey-source.zip")
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

        assertThatThrownBy { downloader.retrievePackage() }
            .isInstanceOf(FileChecksumMismatchException::class.java)
    }

    @Test
    @DisplayName("Constructor should reject blank version and require checksum when verification is enabled")
    fun `constructor validation requires version and checksum`() {
        assertThatThrownBy {
            ValkeyPackageDownloader(
                valkeyVersion = " ",
                operatingSystem = OperatingSystem.LINUX_X86_64,
                binaryPathWithinPackage = Paths.get("bin/valkey-server"),
                archiveType = ArchiveType.ZIP,
                downloadUri = URI("file:///tmp/valkey.zip")
            )
        }.isInstanceOf(IllegalArgumentException::class.java)

        assertThatThrownBy {
            ValkeyPackageDownloader(
                valkeyVersion = "9.0.2",
                operatingSystem = OperatingSystem.LINUX_X86_64,
                binaryPathWithinPackage = Paths.get("bin/valkey-server"),
                archiveType = ArchiveType.ZIP,
                downloadUri = URI("file:///tmp/valkey.zip"),
                sha256FileChecksum = null,
                verifyFileChecksum = true
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest(name = "humanReadableByteCount({0}) should return \"{1}\"")
    @CsvSource(
        "-1, N/A",
        "10, 10 B",
        "1024, 1.0 KiB",
        "1048576, 1.0 MiB",
        "1073741824, 1.0 GiB",
        "1099511627776, 4294967296.0 TiB",
        "1125844931261236, 4294757580.8 PiB",
        "1152865209611504845, 4294757580.8 EiB"
    )
    @DisplayName("humanReadableByteCount() should format byte counts into human-readable strings")
    fun `humanReadableByteCount formats bytes correctly`(bytes: Long, expected: String) {
        assertThat(humanReadableByteCount(bytes)).isEqualTo(expected)
    }

    @Test
    @DisplayName("Default cache path should follow the expected naming layout")
    fun `default cache path builder uses expected layout`() {
        val path = resolveDefaultTempFilePath("9.0.2", OperatingSystem.LINUX_X86_64, ArchiveType.ZIP)
        val pathAsString = path.toString()

        assertThat(pathAsString).contains("valkey-9.0.2-linux_x86_64")
        assertThat(pathAsString).endsWith(".zip")
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
