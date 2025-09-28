package io.github.tobi.laa.embedded.valkey.distribution

import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Represents a Valkey distribution for a specific [version], [operatingSystem], and [architecture]. The
 * [installationPath] points to the directory where the distribution is installed, and the [binaryPath] points to the
 * Valkey server binary within that installation.
 *
 * @param version The version of the Valkey distribution, i.e. `"8.1.3"`.
 * @param operatingSystem The operating system for which the Valkey distribution is built.
 * @param distributionType The type of the Valkey distribution (default is [DistributionType.VALKEY]).
 * @param installationPath The path to the directory where the Valkey distribution is installed.
 * @param binaryPath The path to the Valkey server binary within the installation directory.
 */
data class ValkeyDistribution(
    val version: String,
    val operatingSystem: OperatingSystem,
    val distributionType: DistributionType = DistributionType.VALKEY,
    val installationPath: Path,
    val binaryPath: Path
) {

    init {
        require(version.isNotBlank()) { "Version must not be blank." }
        require(installationPath.exists()) { "Installation path $installationPath must exist." }
        require(installationPath.isDirectory()) { "Installation path $installationPath must be a directory." }
        require(binaryPath.exists()) { "Binary path $binaryPath must exist." }
    }
}
