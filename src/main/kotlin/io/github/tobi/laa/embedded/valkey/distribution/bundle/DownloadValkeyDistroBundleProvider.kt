package io.github.tobi.laa.embedded.valkey.distribution.bundle

import io.github.tobi.laa.embedded.valkey.distribution.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Proxy
import java.net.URI
import java.nio.file.Files.copy
import java.nio.file.Files.newOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

/**
 * Provides a Valkey distribution bundle by downloading it from a remote location.
 *
 * @param valkeyVersion The version of the Valkey distribution, e.g. `"8.1.3"`.
 * @param operatingSystem The operating system for which the Valkey distribution is built.
 * @param distributionType The type of the Valkey distribution (default is [DistributionType.VALKEY]).
 * @param binaryPathWithinBundle The relative path to the Valkey server binary within the downloaded distribution bundle.
 * @param archiveType The type of the archive (e.g., `TAR_GZ`).
 * @param downloadUri The URI from which to download the Valkey distribution bundle.
 * @param proxy The proxy to use for downloading the bundle (defaults to not using a proxy).
 * @param cacheDownload Whether to cache the downloaded bundle for future use (default is `true`).
 * @param cacheFileLocation The path where the cached bundle will be stored (default is a file in the system temp directory).
 * @param sha256FileChecksum The expected SHA-256 checksum of the downloaded bundle for integrity verification (optional).
 * @param verifyFileChecksum Whether to verify the SHA-256 checksum of the downloaded bundle (is enabled by default if [sha256FileChecksum] is provided).
 * @param downloadLocation The path where the downloaded bundle will be stored (defaults to `cacheFileLocation`).
 */
class DownloadValkeyDistroBundleProvider(
    internal val valkeyVersion: String,
    internal val operatingSystem: OperatingSystem,
    internal val distributionType: DistributionType = DistributionType.VALKEY,
    internal val binaryPathWithinBundle: Path,
    internal val archiveType: ArchiveType,
    internal val downloadUri: URI,
    internal val proxy: Proxy = Proxy.NO_PROXY,
    internal val cacheDownload: Boolean = true,
    internal val cacheFileLocation: Path = resolveDefaultTempFilePath(valkeyVersion, operatingSystem, archiveType),
    internal val sha256FileChecksum: String? = null,
    internal val verifyFileChecksum: Boolean = sha256FileChecksum != null,
    internal val downloadLocation: Path = cacheFileLocation
) :
    ValkeyDistributionBundleProvider {

    internal val logger = LoggerFactory.getLogger(DownloadValkeyDistroBundleProvider::class.java)

    init {
        require(valkeyVersion.isNotBlank()) { "Version must not be blank." }
        require(!verifyFileChecksum || !sha256FileChecksum.isNullOrBlank()) { "SHA-256 checksum must be provided if checksum verification is enabled." }
    }

    @Throws(IOException::class)
    override fun provideDistributionBundle(): ValkeyDistributionBundle {
        if (downloadNecessary()) {
            downloadValkeyDistributionBundle()
        }
        if (verifyFileChecksum) {
            verifyValkeyDistributionBundleChecksum()
        }
        copyToDownloadLocation()
        return ValkeyDistributionBundle(
            version = valkeyVersion,
            operatingSystem = operatingSystem,
            distributionType = distributionType,
            bundlePath = downloadLocation,
            binaryPathWithinBundle = binaryPathWithinBundle,
            archiveType = archiveType
        )
    }

    private fun downloadNecessary(): Boolean = !cacheDownload || cacheFileLocation.notExists()

    @Throws(IOException::class)
    internal fun downloadValkeyDistributionBundle() {
        logger.info("🔃 Downloading ${distributionType.displayName} v$valkeyVersion for ${operatingSystem.displayName} from $downloadUri")
        val targetPath = if (cacheDownload) cacheFileLocation else downloadLocation
        val connectionToMirror = downloadUri.toURL().openConnection(proxy)
        val bundleSize = connectionToMirror.contentLengthLong
        var totalBytesRead: Long = 0
        connectionToMirror.getInputStream().use { bundleDownloadStream ->
            targetPath.parent.createDirectories()
            newOutputStream(targetPath, TRUNCATE_EXISTING, CREATE).use { targetStream ->
                totalBytesRead =
                    downloadValkeyDistributionAndLogProgress(bundleDownloadStream, targetStream, bundleSize)
            }
            logger.info(
                "✅ Downloaded ${distributionType.displayName} v$valkeyVersion for ${operatingSystem.displayName} to $targetPath (${
                    humanReadableByteCount(
                        totalBytesRead
                    )
                })"
            )
        }
    }

    private fun downloadValkeyDistributionAndLogProgress(
        bundleDownloadStream: InputStream,
        targetStream: OutputStream,
        bundleSize: Long
    ): Long {
        var totalBytesRead: Long = 0
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var lastLoggedProgress = 0L
        while (bundleDownloadStream.read(buffer).also { bytesRead = it } != -1) {
            targetStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            val progress = (totalBytesRead * 100) / bundleSize
            if (progress - lastLoggedProgress >= 10) {
                logger.info(
                    "🔃 Downloading ${distributionType.displayName} v$valkeyVersion for ${operatingSystem.displayName}: $progress% (${
                        humanReadableByteCount(
                            totalBytesRead
                        )
                    } of ${
                        humanReadableByteCount(
                            bundleSize
                        )
                    })"
                )
                lastLoggedProgress = progress
            }
        }
        targetStream.flush()
        return totalBytesRead
    }

    private fun verifyValkeyDistributionBundleChecksum() {
        val actualSha256Hash = computeSha256Hashsum(if (cacheDownload) cacheFileLocation else downloadLocation)
        if (!actualSha256Hash.equals(sha256FileChecksum, ignoreCase = true)) {
            throw FileChecksumMismatchException(
                "🫆 SHA-256 hash mismatch for ${distributionType.displayName} v$valkeyVersion for ${operatingSystem.displayName} from $downloadUri. Expected: $sha256FileChecksum, Actual: $actualSha256Hash"
            )
        }
        logger.trace(
            "🫆 SHA-256 hash successfully verified for {} v{} for {} from {}.",
            distributionType.displayName,
            valkeyVersion,
            operatingSystem.displayName,
            downloadUri
        )
    }

    private fun computeSha256Hashsum(file: Path): String {
        return MessageDigest.getInstance("SHA-256").let { digest ->
            file.toFile().inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    private fun copyToDownloadLocation() {
        if (cacheDownload && cacheFileLocation != downloadLocation) {
            downloadLocation.parent.createDirectories()
            copy(cacheFileLocation, downloadLocation, StandardCopyOption.REPLACE_EXISTING)
            logger.trace(
                "Copied {} v{} for {} from cache {} to download location {}",
                distributionType.displayName,
                valkeyVersion,
                operatingSystem.displayName,
                cacheFileLocation,
                downloadLocation
            )
        }
    }
}

private fun resolveDefaultTempFilePath(
    valkeyVersion: String,
    operatingSystem: OperatingSystem,
    archiveType: ArchiveType
): Path = Paths.get(
    systemTempDirectory(),
    "valkey-$valkeyVersion-${operatingSystem.name.lowercase()}",
    "valkey-$valkeyVersion-${operatingSystem.name.lowercase()}.${archiveType.fileExtension}"
)

private fun humanReadableByteCount(bytes: Long) = when {
    bytes == Long.MIN_VALUE || bytes < 0 -> "N/A"
    bytes < 1024L -> "$bytes B"
    bytes <= 0xfffccccccccccccL shr 40 -> "%.1f KiB".format(bytes.toDouble() / (0x1 shl 10))
    bytes <= 0xfffccccccccccccL shr 30 -> "%.1f MiB".format(bytes.toDouble() / (0x1 shl 20))
    bytes <= 0xfffccccccccccccL shr 20 -> "%.1f GiB".format(bytes.toDouble() / (0x1 shl 30))
    bytes <= 0xfffccccccccccccL shr 10 -> "%.1f TiB".format(bytes.toDouble() / (0x1 shl 40))
    bytes <= 0xfffccccccccccccL -> "%.1f PiB".format((bytes shr 10).toDouble() / (0x1 shl 40))
    else -> "%.1f EiB".format((bytes shr 20).toDouble() / (0x1 shl 40))
}
