package io.github.tobi.laa.embedded.valkey.distribution.bundle

import io.github.tobi.laa.embedded.valkey.distribution.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.nio.file.Files.copy
import java.nio.file.Files.newOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

/**
 * Provides a Valkey distribution bundle by downloading it from a remote location.
 *
 * @param valkeyVersion The version of the Valkey distribution, e.g. `"8.1.3"`.
 * @param operatingSystem The operating system for which the Valkey distribution is built.
 * @param architecture The architecture for which the Valkey distribution is built.
 * @param distributionType The type of the Valkey distribution (default is [DistributionType.VALKEY]).
 * @param binaryPathWithinBundle The relative path to the Valkey server binary within the downloaded distribution bundle.
 * @param archiveType The type of the archive (e.g., `TAR_GZ`).
 * @param downloadUri The URI from which to download the Valkey distribution bundle.
 * @param cacheDownload Whether to cache the downloaded bundle for future use (default is `true`).
 * @param cacheFileLocation The path where the cached bundle will be stored (default is a file in the system temp directory).
 * @param downloadLocation The path where the downloaded bundle will be stored (defaults to `cacheFileLocation`).
 */
class DownloadValkeyDistroBundleProvider(
    internal val valkeyVersion: String,
    internal val operatingSystem: OperatingSystem,
    internal val distributionType: DistributionType = DistributionType.VALKEY,
    internal val binaryPathWithinBundle: Path,
    internal val archiveType: ArchiveType,
    internal val downloadUri: URI,
    internal val cacheDownload: Boolean = true,
    internal val cacheFileLocation: Path = resolveDefaultTempFilePath(valkeyVersion, operatingSystem, archiveType),
    internal val downloadLocation: Path = cacheFileLocation
) :
    ValkeyDistributionBundleProvider {

    internal val logger = LoggerFactory.getLogger(DownloadValkeyDistroBundleProvider::class.java)

    init {
        require(valkeyVersion.isNotBlank()) { "Version must not be blank." }
    }

    @Throws(IOException::class)
    override fun provideDistributionBundle(): ValkeyDistributionBundle {
        if (downloadNecessary()) {
            downloadValkeyDistributionBundle()
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
        logger.info("Downloading ${distributionType.displayName} v$valkeyVersion for ${operatingSystem.displayName} from $downloadUri")
        val targetPath = if (cacheDownload) cacheFileLocation else downloadLocation
        downloadUri.toURL().openStream().use { httpDownloadStream ->
            targetPath.parent.createDirectories()
            newOutputStream(targetPath, TRUNCATE_EXISTING, CREATE).use { httpDownloadStream.copyTo(it) }
            logger.info("Downloaded ${distributionType.displayName} v$valkeyVersion for ${operatingSystem.displayName} to $targetPath")
        }
    }

    internal fun copyToDownloadLocation() {
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