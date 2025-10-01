package io.github.tobi.laa.embedded.valkey.distribution

import io.github.tobi.laa.embedded.valkey.distribution.bundle.ArchiveType.*
import io.github.tobi.laa.embedded.valkey.distribution.bundle.ValkeyDistributionBundle
import io.github.tobi.laa.embedded.valkey.distribution.bundle.systemTempDirectory
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.inputStream

/**
 * Extracts a Valkey distribution from a given [ValkeyDistributionBundle]. The bundle is extracted to the specified
 * [installationPath]. If no path is provided, a default temporary path is used.
 *
 * @param valkeyDistributionBundle The Valkey distribution bundle to be extracted.
 * @param installationPath The path where the Valkey distribution should be installed. Defaults to a temporary directory.
 * @param alwaysExtract Whether to always extract the Valkey distribution, even if it has already been extracted to the
 * specified installation path. Default is `false`.
 * @param ensureBinaryIsExecutable Whether to ensure that the Valkey binary is executable after extraction. Default is `true`.
 */
class ExtractValkeyDistributionProvider
@JvmOverloads
constructor(
    internal val valkeyDistributionBundle: ValkeyDistributionBundle,
    internal val installationPath: Path = resolveDefaultTempInstallationPath(
        valkeyDistributionBundle.distributionType,
        valkeyDistributionBundle.version,
        valkeyDistributionBundle.operatingSystem
    ),
    internal val alwaysExtract: Boolean = false,
    internal val ensureBinaryIsExecutable: Boolean = true
) :
    ValkeyDistributionProvider {

    internal val logger = LoggerFactory.getLogger(ExtractValkeyDistributionProvider::class.java)

    @Throws(IOException::class)
    override fun provideDistribution(): ValkeyDistribution {
        if (extractionNeeded()) {
            extractValkeyDistributionBundle()
        }
        if (ensureBinaryIsExecutable) {
            makeBinaryExecutable()
        }
        return ValkeyDistribution(
            version = valkeyDistributionBundle.version,
            operatingSystem = valkeyDistributionBundle.operatingSystem,
            distributionType = valkeyDistributionBundle.distributionType,
            installationPath = installationPath,
            binaryPath = locateBinary()
        )
    }

    private fun extractionNeeded() = alwaysExtract || !Files.exists(locateBinary())

    private fun extractValkeyDistributionBundle() {
        logger.trace(
            "Extract {} v{} for {} to {}",
            valkeyDistributionBundle.distributionType.displayName,
            valkeyDistributionBundle.version,
            valkeyDistributionBundle.operatingSystem.displayName,
            installationPath
        )
        when (valkeyDistributionBundle.archiveType) {
            TAR_GZ -> extractTarGzip()
            TAR_BZ2 -> extractTarBZip2()
            ZIP -> extractZip()
        }
        logger.trace(
            "Successfully extracted {} v{} for {} to {}",
            valkeyDistributionBundle.distributionType.displayName,
            valkeyDistributionBundle.version,
            valkeyDistributionBundle.operatingSystem.displayName,
            installationPath
        )
    }

    private fun extractTarGzip() {
        valkeyDistributionBundle.bundlePath.inputStream().use { bundleStream ->
            GzipCompressorInputStream(bundleStream).use { gzipStream ->
                TarArchiveInputStream(gzipStream).use {
                    extractArchive(it, installationPath)
                }
            }
        }
    }

    internal fun extractTarBZip2() {
        valkeyDistributionBundle.bundlePath.inputStream().use { bundleStream ->
            BZip2CompressorInputStream(bundleStream).use { gzipStream ->
                TarArchiveInputStream(gzipStream).use {
                    extractArchive(it, installationPath)
                }
            }
        }
    }

    internal fun extractZip() {
        valkeyDistributionBundle.bundlePath.inputStream().use { bundleStream ->
            ZipArchiveInputStream(bundleStream).use {
                extractArchive(it, installationPath)
            }
        }
    }

    internal fun <E : ArchiveEntry> extractArchive(archiveStream: ArchiveInputStream<E>, targetDirectory: Path) {
        var entry: ArchiveEntry? = archiveStream.nextEntry
        while (entry != null) {
            val extractTo: Path = targetDirectory.resolve(entry.name)
            if (entry.isDirectory) {
                Files.createDirectories(extractTo)
            } else {
                Files.createDirectories(extractTo.parent) // seems to be necessary for zip files
                Files.copy(archiveStream, extractTo, REPLACE_EXISTING)
            }
            entry = archiveStream.nextEntry
        }
    }

    private fun locateBinary(): Path {
        return installationPath.resolve(valkeyDistributionBundle.binaryPathWithinBundle)
    }

    private fun makeBinaryExecutable() {
        val binaryPath = locateBinary()
        if (!binaryPath.toFile().setExecutable(true)) {
            throw IOException("Failed to make ${valkeyDistributionBundle.distributionType.displayName} binary at $binaryPath executable.")
        }
    }
}

internal fun resolveDefaultTempInstallationPath(
    distributionType: DistributionType,
    valkeyVersion: String,
    operatingSystem: OperatingSystem
): Path = Paths.get(
    systemTempDirectory(),
    "${distributionType.name.lowercase()}-$valkeyVersion-${operatingSystem.name.lowercase()}",
    "${distributionType.name.lowercase()}-$valkeyVersion-${operatingSystem.name.lowercase()}"
)