package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Represents a Valkey distribution bundle for a specific [version] and [operatingSystem]. The [path] points to an
 * archive file containing the Valkey distribution.
 *
 * @param version The version of the Valkey distribution, i.e. `"8.1.3"`.
 * @param operatingSystem The operating system for which the Valkey distribution is built.
 * @param distributionType The type of the Valkey distribution (default is [DistributionType.VALKEY]).
 * @param path The path to the Valkey distribution bundle (archive).
 * @param binaryPathWithinPackage The relative path to the Valkey server binary *within the bundle's archive*.
 * @param archiveType The type of the archive (e.g., `TAR_GZ`).
 */
data class ValkeyPackage
@JvmOverloads
constructor(
    val version: String,
    val operatingSystem: OperatingSystem,
    val distributionType: DistributionType = DistributionType.VALKEY,
    val path: Path,
    val binaryPathWithinPackage: Path,
    val archiveType: ArchiveType
) {

    init {
        require(version.isNotBlank()) { "Version must not be blank." }
        require(path.exists()) { "Path to bundle $path must exist." }
        require(!binaryPathWithinPackage.isAbsolute) { "Binary path $binaryPathWithinPackage within bundle must be relative." }
    }
}
