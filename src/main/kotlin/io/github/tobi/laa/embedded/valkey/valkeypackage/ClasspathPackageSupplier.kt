package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.toPath

/**
 * Loads a Valkey package from a classpath resource.
 *
 * @param classpathResource The classpath resource path to the Valkey package, e.g. `"/valkey/valkey-8.1.3-linux-x86_64.tar.gz"`.
 * @param valkeyVersion The version of the Valkey installation bundled within the package, e.g. `"8.1.3"`.
 * @param operatingSystem The operating system for which the Valkey installation is built.
 * @param binaryPathWithinPackage The relative path to the Valkey server binary within the package.
 * @param archiveType The type of the archive (e.g., `TAR_GZ`).
 * @param distributionType The type of the Valkey installation (default is [DistributionType.VALKEY]).
 */
class ClasspathPackageSupplier
@JvmOverloads
constructor(
    internal val classpathResource: String,
    internal val valkeyVersion: String,
    internal val operatingSystem: OperatingSystem,
    internal val binaryPathWithinPackage: Path,
    internal val archiveType: ArchiveType,
    internal val distributionType: DistributionType = DistributionType.VALKEY,
) :
    ValkeyPackageSupplier {

    internal val log = LoggerFactory.getLogger(ClasspathPackageSupplier::class.java)

    init {
        require(valkeyVersion.isNotBlank()) { "Version must not be blank." }
    }

    @Throws(IOException::class)
    override fun retrievePackage(): ValkeyPackage {
        val bundlePath =
            this::class.java.getResource(classpathResource)?.toURI()?.toPath()
                ?: throw IOException("Failed to load Valkey package from classpath resource '$classpathResource'.")
        return ValkeyPackage(
            version = valkeyVersion,
            operatingSystem = operatingSystem,
            distributionType = distributionType,
            path = bundlePath,
            binaryPathWithinPackage = binaryPathWithinPackage,
            archiveType = archiveType
        )
    }
}
