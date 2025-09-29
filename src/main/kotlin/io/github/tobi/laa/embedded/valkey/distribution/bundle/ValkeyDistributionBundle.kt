package io.github.tobi.laa.embedded.valkey.distribution.bundle

import io.github.tobi.laa.embedded.valkey.distribution.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Represents a Valkey distribution bundle for a specific [version] and [operatingSystem]. The [bundlePath] points to an
 * archive file containing the Valkey distribution.
 *
 * @param version The version of the Valkey distribution, i.e. `"8.1.3"`.
 * @param operatingSystem The operating system for which the Valkey distribution is built.
 * @param distributionType The type of the Valkey distribution (default is [DistributionType.VALKEY]).
 * @param bundlePath The path to the Valkey distribution bundle (archive).
 * @param binaryPathWithinBundle The relative path to the Valkey server binary *within the bundle's archive*.
 * @param archiveType The type of the archive (e.g., `TAR_GZ`).
 */
data class ValkeyDistributionBundle(
    val version: String,
    val operatingSystem: OperatingSystem,
    val distributionType: DistributionType = DistributionType.VALKEY,
    val bundlePath: Path,
    val binaryPathWithinBundle: Path,
    val archiveType: ArchiveType
) {

    init {
        require(version.isNotBlank()) { "Version must not be blank." }
        require(bundlePath.exists()) { "Path to bundle $bundlePath must exist." }
        require(!binaryPathWithinBundle.isAbsolute) { "Binary path $binaryPathWithinBundle within bundle must be relative." }
    }
}
