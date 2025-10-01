package io.github.tobi.laa.embedded.valkey.distribution.bundle

import io.github.tobi.laa.embedded.valkey.distribution.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.toPath

/**
 * Provides a Valkey distribution bundle by retrieving it from the classpath.
 *
 * @param classpathResource The classpath resource path to the Valkey distribution bundle, e.g. `"/valkey/valkey-8.1.3-linux-x86_64.tar.gz"`.
 * @param valkeyVersion The version of the Valkey distribution, e.g. `"8.1.3"`.
 * @param operatingSystem The operating system for which the Valkey distribution is built.
 * @param binaryPathWithinBundle The relative path to the Valkey server binary within the downloaded distribution bundle.
 * @param archiveType The type of the archive (e.g., `TAR_GZ`).
 * @param distributionType The type of the Valkey distribution (default is [DistributionType.VALKEY]).
 */
class ClasspathValkeyDistroBundleProvider
@JvmOverloads
constructor(
    internal val classpathResource: String,
    internal val valkeyVersion: String,
    internal val operatingSystem: OperatingSystem,
    internal val binaryPathWithinBundle: Path,
    internal val archiveType: ArchiveType,
    internal val distributionType: DistributionType = DistributionType.VALKEY,
) :
    ValkeyDistributionBundleProvider {

    internal val logger = LoggerFactory.getLogger(ClasspathValkeyDistroBundleProvider::class.java)

    init {
        require(valkeyVersion.isNotBlank()) { "Version must not be blank." }
    }

    @Throws(IOException::class)
    override fun provideDistributionBundle(): ValkeyDistributionBundle {
        val bundlePath =
            this::class.java.getResource(classpathResource)?.toURI()?.toPath()
                ?: throw IOException("Failed to load Valkey distribution bundle from classpath resource '$classpathResource'.")
        return ValkeyDistributionBundle(
            version = valkeyVersion,
            operatingSystem = operatingSystem,
            distributionType = distributionType,
            bundlePath = bundlePath,
            binaryPathWithinBundle = binaryPathWithinBundle,
            archiveType = archiveType
        )
    }
}
