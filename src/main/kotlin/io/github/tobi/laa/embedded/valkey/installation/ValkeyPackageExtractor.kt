package io.github.tobi.laa.embedded.valkey.installation

import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import io.github.tobi.laa.embedded.valkey.valkeypackage.ArchiveType.TAR_BZ2
import io.github.tobi.laa.embedded.valkey.valkeypackage.ArchiveType.TAR_GZ
import io.github.tobi.laa.embedded.valkey.valkeypackage.ArchiveType.ZIP
import io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackage
import io.github.tobi.laa.embedded.valkey.valkeypackage.systemTempDirectory
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
import kotlin.io.path.absolute
import kotlin.io.path.inputStream

/**
 * Extracts a Valkey distribution from a given [ValkeyPackage]. The bundle is extracted to the specified
 * [installationPath]. If no path is provided, a default temporary path is used.
 *
 * @param valkeyPackage The Valkey distribution bundle to be extracted.
 * @param installationPath The path where the Valkey distribution should be installed. Defaults to a temporary directory.
 * @param alwaysExtract Whether to always extract the Valkey distribution, even if it has already been extracted to the
 * specified installation path. Default is `false`.
 * @param ensureBinaryIsExecutable Whether to ensure that the Valkey binary is executable after extraction. Default is `true`.
 */
class ValkeyPackageExtractor
@JvmOverloads
constructor(
    internal val valkeyPackage: ValkeyPackage,
    internal val installationPath: Path = resolveDefaultTempInstallationPath(
        valkeyPackage.distributionType,
        valkeyPackage.version,
        valkeyPackage.operatingSystem
    ),
    internal val alwaysExtract: Boolean = false,
    internal val ensureBinaryIsExecutable: Boolean = true
) :
    ValkeyInstallationSupplier {

    internal val logger = LoggerFactory.getLogger(ValkeyPackageExtractor::class.java)

    @Throws(IOException::class)
    override fun installValkey(): ValkeyInstallation {
        if (extractionNeeded()) {
            extractValkeyDistributionBundle()
        }
        if (ensureBinaryIsExecutable) {
            makeBinaryExecutable()
        }
        return ValkeyInstallation(
            version = valkeyPackage.version,
            operatingSystem = valkeyPackage.operatingSystem,
            distributionType = valkeyPackage.distributionType,
            installationPath = installationPath,
            binaryPath = locateBinary()
        )
    }

    private fun extractionNeeded() = alwaysExtract || !Files.exists(locateBinary())

    private fun extractValkeyDistributionBundle() {
        logger.trace(
            "Extract {} v{} for {} to {}",
            valkeyPackage.distributionType.displayName,
            valkeyPackage.version,
            valkeyPackage.operatingSystem.displayName,
            installationPath
        )
        when (valkeyPackage.archiveType) {
            TAR_GZ -> extractTarGzip()
            TAR_BZ2 -> extractTarBZip2()
            ZIP -> extractZip()
        }
        logger.trace(
            "Successfully extracted {} v{} for {} to {}",
            valkeyPackage.distributionType.displayName,
            valkeyPackage.version,
            valkeyPackage.operatingSystem.displayName,
            installationPath
        )
    }

    private fun extractTarGzip() {
        valkeyPackage.path.inputStream().use { bundleStream ->
            GzipCompressorInputStream(bundleStream).use { gzipStream ->
                TarArchiveInputStream(gzipStream).use {
                    extractArchive(it, installationPath)
                }
            }
        }
    }

    internal fun extractTarBZip2() {
        valkeyPackage.path.inputStream().use { bundleStream ->
            BZip2CompressorInputStream(bundleStream).use { gzipStream ->
                TarArchiveInputStream(gzipStream).use {
                    extractArchive(it, installationPath)
                }
            }
        }
    }

    internal fun extractZip() {
        valkeyPackage.path.inputStream().use { bundleStream ->
            ZipArchiveInputStream(bundleStream).use {
                extractArchive(it, installationPath)
            }
        }
    }

    internal fun <E : ArchiveEntry> extractArchive(archiveStream: ArchiveInputStream<E>, targetDirectory: Path) {
        var entry: ArchiveEntry? = archiveStream.nextEntry
        while (entry != null) {
            val extractTo: Path = targetDirectory.resolve(entry.name).absolute()
            if (!extractTo.startsWith(targetDirectory)) {
                throw IOException("Zip (or archive) slip detected. Extracting ${entry.name} would lead to it being placed outside of $targetDirectory")
            } else if (entry.isDirectory) {
                Files.createDirectories(extractTo)
            } else {
                Files.createDirectories(extractTo.parent) // seems to be necessary for zip files
                Files.copy(archiveStream, extractTo, REPLACE_EXISTING)
            }
            entry = archiveStream.nextEntry
        }
    }

    private fun locateBinary(): Path {
        return installationPath.resolve(valkeyPackage.binaryPathWithinPackage)
    }

    private fun makeBinaryExecutable() {
        val binaryPath = locateBinary()
        if (!binaryPath.toFile().setExecutable(true)) {
            throw IOException("Failed to make ${valkeyPackage.distributionType.displayName} binary at $binaryPath executable.")
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