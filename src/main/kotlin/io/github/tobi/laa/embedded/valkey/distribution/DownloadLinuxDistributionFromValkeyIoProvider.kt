package io.github.tobi.laa.embedded.valkey.distribution

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.slf4j.LoggerFactory
import redis.embedded.model.Architecture
import redis.embedded.model.OS.UNIX
import java.net.URI
import java.nio.file.Files
import java.nio.file.Files.newOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.notExists

const val DEFAULT_VALKEY_LINUX_VERSION = "8.1.3"

/**
 * Downloads and extracts a Valkey distribution for Linux from the official Valkey website.
 */
class DownloadLinuxDistributionFromValkeyIoProvider(
    internal val architecture: Architecture,
    internal val valkeyVersion: String = DEFAULT_VALKEY_LINUX_VERSION,
    internal val installationPath: Path = Files.createTempDirectory("valkey-$valkeyVersion-jammy-${architecture.name.lowercase()}"),
    internal val forceDownload: Boolean = false,
    cacheDirectory: Path = Paths.get(
        System.getProperty("java.io.tmpdir"),
        "valkey-$valkeyVersion-jammy-${architecture.name.lowercase()}"
    )
) :
    ValkeyDistributionProvider {

    internal val logger = LoggerFactory.getLogger(DownloadLinuxDistributionFromValkeyIoProvider::class.java)

    internal val distroFilename = "valkey-$valkeyVersion-jammy-${architecture.name.lowercase()}.tar.gz"
    internal val downloadUri = URI("https://download.valkey.io/releases/$distroFilename")

    internal val cachedFile = cacheDirectory.resolve(distroFilename)

    internal val binaryPath = "valkey-$valkeyVersion-jammy-${architecture.name.lowercase()}/bin/valkey-server"

    override fun provideDistribution(): ValkeyDistribution {
        if (forceDownload || cachedFile.notExists()) {
            downloadValkeyDistribution()
        }
        extractValkeyDistribution()
        return ValkeyDistribution(
            version = valkeyVersion,
            operatingSystem = UNIX,
            architecture = architecture,
            installationPath = installationPath,
            binaryPath = locateBinary()
        )
    }

    internal fun downloadValkeyDistribution() {
        logger.info("Downloading Valkey $valkeyVersion for architecture ${architecture.name} from $downloadUri")
        downloadUri.toURL().openStream().use { httpDownloadStream ->
            cachedFile.parent.createDirectories()
            newOutputStream(cachedFile, TRUNCATE_EXISTING, CREATE).use { httpDownloadStream.copyTo(it) }
            logger.info("Downloaded Valkey to $cachedFile")
        }
    }

    internal fun extractValkeyDistribution() {
        logger.info("Extract Valkey $valkeyVersion for architecture ${architecture.name} to $installationPath")
        cachedFile.inputStream().use { cachedFileStream ->
            GzipCompressorInputStream(cachedFileStream).use { gzipStream ->
                TarArchiveInputStream(gzipStream).use {
                    extractTarGzip(it, installationPath)
                }
            }
        }
        logger.info("Downloaded and extracted Valkey to $installationPath")
    }

    internal fun extractTarGzip(tarStream: TarArchiveInputStream, targetDirectory: Path) {
        var entry: ArchiveEntry? = tarStream.nextEntry
        while (entry != null) {
            val extractTo: Path = targetDirectory.resolve(entry.getName())
            if (entry.isDirectory) {
                Files.createDirectories(extractTo)
            } else {
                Files.copy(tarStream, extractTo, REPLACE_EXISTING)
            }
            entry = tarStream.nextEntry
        }
    }

    internal fun locateBinary(): Path {
        return installationPath.resolve(binaryPath)
    }
}