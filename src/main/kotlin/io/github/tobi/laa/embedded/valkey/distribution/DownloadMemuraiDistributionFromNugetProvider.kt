package io.github.tobi.laa.embedded.valkey.distribution

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.slf4j.LoggerFactory
import redis.embedded.model.Architecture
import redis.embedded.model.OS.WINDOWS
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

const val DEFAULT_MEMURAI_VERSION = "4.1.6"

/**
 * Downloads and extracts a Memurai distribution for Windows from Nuget.
 */
class DownloadMemuraiDistributionFromNugetProvider(
    internal val architecture: Architecture,
    internal val memuraiVersion: String = DEFAULT_MEMURAI_VERSION,
    internal val installationPath: Path = Files.createTempDirectory("memurai-developer-$memuraiVersion-${architecture.name.lowercase()}"),
    internal val forceDownload: Boolean = false,
    cacheDirectory: Path = Paths.get(
        System.getProperty("java.io.tmpdir"),
        "memurai-developer-$memuraiVersion-${architecture.name.lowercase()}"
    )
) :
    ValkeyDistributionProvider {

    internal val logger = LoggerFactory.getLogger(DownloadMemuraiDistributionFromNugetProvider::class.java)

    internal val downloadUri = URI("https://www.nuget.org/api/v2/package/MemuraiDeveloper/$memuraiVersion")

    internal val cachedFile = cacheDirectory.resolve("memuraideveloper.$memuraiVersion.nupkg")

    internal val binaryPath = "tools/memurai.exe"

    override fun provideDistribution(): ValkeyDistribution {
        if (forceDownload || cachedFile.notExists()) {
            downloadMemuraiDistribution()
        }
        extractMemuraiDistribution()
        return ValkeyDistribution(
            version = memuraiVersion,
            operatingSystem = WINDOWS,
            architecture = architecture,
            installationPath = installationPath,
            binaryPath = locateBinary()
        )
    }

    internal fun downloadMemuraiDistribution() {
        logger.info("Downloading Memurai Developer $memuraiVersion for architecture ${architecture.name} from $downloadUri")
        downloadUri.toURL().openStream().use { httpDownloadStream ->
            cachedFile.parent.createDirectories()
            newOutputStream(cachedFile, TRUNCATE_EXISTING, CREATE).use { httpDownloadStream.copyTo(it) }
            logger.info("Downloaded Memurai Developer to $cachedFile")
        }
    }

    internal fun extractMemuraiDistribution() {
        logger.info("Extract Memurai Developer $memuraiVersion for architecture ${architecture.name} to $installationPath")
        cachedFile.inputStream().use { cachedFileStream ->
            ZipArchiveInputStream(cachedFileStream).use {
                extractZip(it, installationPath)
            }
        }
        logger.info("Downloaded and extracted Memurai Developer to $installationPath")
    }

    internal fun extractZip(zipStream: ZipArchiveInputStream, targetDirectory: Path) {
        var entry: ArchiveEntry? = zipStream.nextEntry
        while (entry != null) {
            val extractTo: Path = targetDirectory.resolve(entry.getName())
            if (entry.isDirectory) {
                Files.createDirectories(extractTo)
            } else {
                Files.copy(zipStream, extractTo, REPLACE_EXISTING)
            }
            entry = zipStream.nextEntry
        }
    }

    internal fun locateBinary(): Path {
        return installationPath.resolve(binaryPath)
    }
}