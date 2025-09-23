package io.github.tobi.laa.embedded.valkey.distribution

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.slf4j.LoggerFactory
import redis.embedded.model.Architecture
import redis.embedded.model.OS.MAC_OS_X
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

const val DEFAULT_VALKEY_MAC_OS_VERSION = "8.1.3"

internal val DEFAULT_MAC_OS_BUILD_FILE_PATHS =
    mapOf(
        Architecture.X86_64 to "valkey-${DEFAULT_VALKEY_MAC_OS_VERSION}_0.darwin_24.x86_64.tbz2",
        Architecture.ARM64 to "valkey-${DEFAULT_VALKEY_MAC_OS_VERSION}_0.darwin_25.arm64.tbz2"
    )

/**
 * Downloads and extracts a Valkey distribution for macOS from MacPorts.
 */
class DownloadMacOsDistributionFromMacportsProvider(
    internal val architecture: Architecture,
    internal val valkeyVersion: String = DEFAULT_VALKEY_MAC_OS_VERSION,
    internal val installationPath: Path = Files.createTempDirectory("valkey-$valkeyVersion-darwin-${architecture.name.lowercase()}"),
    internal val buildFilePath: String = DEFAULT_MAC_OS_BUILD_FILE_PATHS[architecture]
        ?: error("No default Valkey macOS path for architecture ${architecture.name} available. Please provide a valid buildFilePath."),
    internal val forceDownload: Boolean = false,
    cacheDirectory: Path = Paths.get(
        System.getProperty("java.io.tmpdir"),
        "valkey-$valkeyVersion-darwin-${architecture.name.lowercase()}"
    )
) :
    ValkeyDistributionProvider {

    internal val logger = LoggerFactory.getLogger(DownloadMacOsDistributionFromMacportsProvider::class.java)

    internal val downloadUri = URI("https://packages.macports.com/valkey/$buildFilePath")

    internal val cachedFile = cacheDirectory.resolve(buildFilePath)

    internal val binaryPath = "opt/local/bin/valkey-server"

    override fun provideDistribution(): ValkeyDistribution {
        if (forceDownload || cachedFile.notExists()) {
            downloadValkeyDistribution()
        }
        extractValkeyDistribution()
        return ValkeyDistribution(
            version = valkeyVersion,
            operatingSystem = MAC_OS_X,
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
        logger.debug("Extract Valkey {} for architecture {} to {}", valkeyVersion, architecture.name, installationPath)
        cachedFile.inputStream().use { cachedFileStream ->
            BZip2CompressorInputStream(cachedFileStream).use { gzipStream ->
                TarArchiveInputStream(gzipStream).use {
                    extractTarBZip2(it, installationPath)
                }
            }
        }
        logger.debug("Extracted Valkey to {}", installationPath)
    }

    internal fun extractTarBZip2(tarStream: TarArchiveInputStream, targetDirectory: Path) {
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