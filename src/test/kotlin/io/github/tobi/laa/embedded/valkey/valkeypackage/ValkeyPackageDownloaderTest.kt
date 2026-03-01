package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.net.URI
import java.nio.file.*
import java.security.MessageDigest

@DisplayName("Tests for ValkeyPackageDownloader")
class ValkeyPackageDownloaderTest {

    companion object {
        private val BINARY_PATH: Path = Paths.get("bin/valkey-server")
        private val DUMMY_DOWNLOAD_URI: URI = URI("file:///tmp/valkey.zip")
    }

    @field:TempDir
    private lateinit var tempDir: Path

    private var givenSourceFile: Path? = null
    private var givenContent: ByteArray? = null
    private var givenCacheFile: Path? = null
    private var givenDownloadLocation: Path? = null
    private var givenChecksum: String? = null
    private var givenDownloader: ValkeyPackageDownloader? = null
    private var retrievePackage: ThrowableAssert.ThrowingCallable? = null
    private var retrievedPackage: ValkeyPackage? = null

    @BeforeEach
    fun reset() {
        givenSourceFile = null
        givenContent = null
        givenCacheFile = null
        givenDownloadLocation = null
        givenChecksum = null
        givenDownloader = null
        retrievePackage = null
        retrievedPackage = null
    }

    @Test
    @DisplayName("Downloaded package should be cached and verified against its checksum")
    fun `downloads package with checksum verification and copies from cache`() {
        givenSourceFileWithContent("payload")
        givenCacheFileLocation()
        givenDownloadLocation()
        givenDownloaderWithCachingAndChecksumVerification()
        whenPackageIsRetrieved()
        thenNoErrorOccurs()
        thenCacheFileExists()
        thenDownloadLocationExists()
        thenPackagePathIsDownloadLocation()
        thenDownloadedFileMatchesChecksum()
    }

    @Test
    @DisplayName("Cached file should be used without re-downloading")
    fun `uses cached file without downloading`() {
        givenCachedFileWithContent("cached")
        givenDownloadLocation()
        givenDownloaderUsingCacheWithNonExistentUri()
        whenPackageIsRetrieved()
        thenNoErrorOccurs()
        thenPackagePathIsDownloadLocation()
        thenDownloadLocationExists()
    }

    @Test
    @DisplayName("Download without caching should write directly to the download location")
    fun `download without cache writes to download location`() {
        givenSourceFileWithContent("payload")
        givenDownloadLocation()
        givenDownloaderWithoutCaching()
        whenPackageIsRetrieved()
        thenNoErrorOccurs()
        thenPackagePathIsDownloadLocation()
        thenDownloadLocationExists()
    }

    @Test
    @DisplayName("Download without caching but with checksum verification should verify the file")
    fun `download without cache but with checksum verification`() {
        givenSourceFileWithContent("payload")
        givenDownloadLocation()
        givenDownloaderWithoutCachingButWithChecksumVerification()
        whenPackageIsRetrieved()
        thenNoErrorOccurs()
        thenPackagePathIsDownloadLocation()
        thenDownloadLocationExists()
        thenDownloadedFileMatchesChecksum()
    }

    @Test
    @DisplayName("Checksum mismatch should throw FileChecksumMismatchException")
    fun `checksum mismatch throws exception`() {
        givenSourceFileWithContent("payload")
        givenDownloaderWithWrongChecksum()
        whenPackageIsRetrieved()
        thenChecksumMismatchExceptionIsThrown()
    }

    @Test
    @DisplayName("Constructor should reject blank version")
    fun `constructor rejects blank version`() {
        givenDownloaderConstructionWithBlankVersion()
        thenIllegalArgumentExceptionIsThrown("Version must not be blank.")
    }

    @Test
    @DisplayName("Constructor should reject null checksum when verification is enabled")
    fun `constructor rejects null checksum when verification is enabled`() {
        givenDownloaderConstructionWithNullChecksumAndVerificationEnabled()
        thenIllegalArgumentExceptionIsThrown("SHA-256 checksum must be provided if checksum verification is enabled.")
    }

    @Test
    @DisplayName("Constructor should reject blank checksum when verification is enabled")
    fun `constructor rejects blank checksum when verification is enabled`() {
        givenDownloaderConstructionWithBlankChecksumAndVerificationEnabled()
        thenIllegalArgumentExceptionIsThrown("SHA-256 checksum must be provided if checksum verification is enabled.")
    }

    @ParameterizedTest(name = "humanReadableByteCount({0}) should return \"{1}\"")
    @CsvSource(
        "-1, N/A",
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
        val path = resolveDefaultTempFilePath("9.0.3", OperatingSystem.LINUX_X86_64, ArchiveType.ZIP)
        val pathAsString = path.toString()

        assertThat(pathAsString).contains("valkey-9.0.3-linux_x86_64")
        assertThat(pathAsString).endsWith(".zip")
    }

    // --- given ---

    private fun givenSourceFileWithContent(content: String) {
        givenContent = content.toByteArray()
        givenSourceFile = tempDir.resolve("valkey-source.zip")
        Files.write(givenSourceFile!!, givenContent!!)
        givenChecksum = sha256(givenContent!!)
    }

    private fun givenCacheFileLocation() {
        givenCacheFile = tempDir.resolve("cache").resolve("valkey-cache.zip")
        Files.createDirectories(givenCacheFile!!.parent)
    }

    private fun givenDownloadLocation() {
        givenDownloadLocation = tempDir.resolve("download").resolve("valkey.zip")
        Files.createDirectories(givenDownloadLocation!!.parent)
    }

    private fun givenCachedFileWithContent(content: String) {
        givenCacheFile = tempDir.resolve("cache").resolve("valkey-cache.zip")
        Files.createDirectories(givenCacheFile!!.parent)
        Files.write(givenCacheFile!!, content.toByteArray())
    }

    private fun givenDownloaderWithCachingAndChecksumVerification() {
        givenDownloader = ValkeyPackageDownloader(
            valkeyVersion = "9.0.3",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            binaryPathWithinPackage = BINARY_PATH,
            archiveType = ArchiveType.ZIP,
            downloadUri = givenSourceFile!!.toUri(),
            cacheFileLocation = givenCacheFile!!,
            downloadLocation = givenDownloadLocation!!,
            sha256FileChecksum = givenChecksum,
            verifyFileChecksum = true,
            distributionType = DistributionType.VALKEY
        )
    }

    private fun givenDownloaderUsingCacheWithNonExistentUri() {
        givenDownloader = ValkeyPackageDownloader(
            valkeyVersion = "9.0.3",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            binaryPathWithinPackage = BINARY_PATH,
            archiveType = ArchiveType.ZIP,
            downloadUri = URI("file:///does-not-exist"),
            cacheFileLocation = givenCacheFile!!,
            downloadLocation = givenDownloadLocation!!,
            cacheDownload = true
        )
    }

    private fun givenDownloaderWithoutCaching() {
        givenDownloader = ValkeyPackageDownloader(
            valkeyVersion = "9.0.3",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            binaryPathWithinPackage = BINARY_PATH,
            archiveType = ArchiveType.ZIP,
            downloadUri = givenSourceFile!!.toUri(),
            cacheDownload = false,
            downloadLocation = givenDownloadLocation!!
        )
    }

    private fun givenDownloaderWithoutCachingButWithChecksumVerification() {
        givenDownloader = ValkeyPackageDownloader(
            valkeyVersion = "9.0.3",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            binaryPathWithinPackage = BINARY_PATH,
            archiveType = ArchiveType.ZIP,
            downloadUri = givenSourceFile!!.toUri(),
            cacheDownload = false,
            downloadLocation = givenDownloadLocation!!,
            sha256FileChecksum = givenChecksum,
            verifyFileChecksum = true
        )
    }

    private fun givenDownloaderWithWrongChecksum() {
        givenDownloader = ValkeyPackageDownloader(
            valkeyVersion = "9.0.3",
            operatingSystem = OperatingSystem.LINUX_X86_64,
            binaryPathWithinPackage = BINARY_PATH,
            archiveType = ArchiveType.ZIP,
            downloadUri = givenSourceFile!!.toUri(),
            sha256FileChecksum = "deadbeef",
            verifyFileChecksum = true
        )
    }

    private fun givenDownloaderConstructionWithBlankVersion() {
        retrievePackage = ThrowableAssert.ThrowingCallable {
            ValkeyPackageDownloader(
                valkeyVersion = " ",
                operatingSystem = OperatingSystem.LINUX_X86_64,
                binaryPathWithinPackage = BINARY_PATH,
                archiveType = ArchiveType.ZIP,
                downloadUri = DUMMY_DOWNLOAD_URI
            )
        }
    }

    private fun givenDownloaderConstructionWithNullChecksumAndVerificationEnabled() {
        retrievePackage = ThrowableAssert.ThrowingCallable {
            ValkeyPackageDownloader(
                valkeyVersion = "9.0.3",
                operatingSystem = OperatingSystem.LINUX_X86_64,
                binaryPathWithinPackage = BINARY_PATH,
                archiveType = ArchiveType.ZIP,
                downloadUri = DUMMY_DOWNLOAD_URI,
                sha256FileChecksum = null,
                verifyFileChecksum = true
            )
        }
    }

    private fun givenDownloaderConstructionWithBlankChecksumAndVerificationEnabled() {
        retrievePackage = ThrowableAssert.ThrowingCallable {
            ValkeyPackageDownloader(
                valkeyVersion = "9.0.3",
                operatingSystem = OperatingSystem.LINUX_X86_64,
                binaryPathWithinPackage = BINARY_PATH,
                archiveType = ArchiveType.ZIP,
                downloadUri = DUMMY_DOWNLOAD_URI,
                sha256FileChecksum = "  ",
                verifyFileChecksum = true
            )
        }
    }

    // --- when ---

    private fun whenPackageIsRetrieved() {
        retrievePackage = ThrowableAssert.ThrowingCallable {
            retrievedPackage = givenDownloader!!.retrievePackage()
        }
    }

    // --- then ---

    private fun thenNoErrorOccurs() {
        assertThatCode(retrievePackage!!).doesNotThrowAnyException()
    }

    private fun thenCacheFileExists() {
        assertThat(givenCacheFile).exists()
    }

    private fun thenDownloadLocationExists() {
        assertThat(givenDownloadLocation).exists()
    }

    private fun thenPackagePathIsDownloadLocation() {
        assertThat(retrievedPackage!!.path).isEqualTo(givenDownloadLocation)
    }

    private fun thenDownloadedFileMatchesChecksum() {
        assertThat(sha256(Files.readAllBytes(givenDownloadLocation!!))).isEqualTo(givenChecksum)
    }

    private fun thenChecksumMismatchExceptionIsThrown() {
        assertThatCode(retrievePackage!!).isInstanceOf(FileChecksumMismatchException::class.java)
    }

    private fun thenIllegalArgumentExceptionIsThrown(expectedMessage: String) {
        assertThatCode(retrievePackage!!).isExactlyInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining(expectedMessage)
    }

    // --- helpers ---

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
