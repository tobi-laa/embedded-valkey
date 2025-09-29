package io.github.tobi.laa.embedded.valkey.distribution.bundle

import io.github.tobi.laa.embedded.valkey.distribution.ExtractValkeyDistributionProvider
import io.github.tobi.laa.embedded.valkey.distribution.ValkeyDistribution
import io.github.tobi.laa.embedded.valkey.distribution.ValkeyDistributionProvider
import io.github.tobi.laa.embedded.valkey.distribution.resolveDefaultTempInstallationPath
import java.io.IOException
import java.nio.file.Path

/**
 * Provides Valkey distribution bundles, i.e., archives containing the Valkey server binary and potentially other
 * files.
 */
interface ValkeyDistributionBundleProvider {

    /**
     * Provides a Valkey distribution bundle. The exact steps to obtain the bundle are implementation-specific. This
     * may involve downloading an archive from the internet or locating it on the local filesystem or in the classpath.
     *
     * @return A [ValkeyDistributionBundle] containing information about the provided distribution bundle, including
     * the path to the archive.
     *
     * @throws java.io.IOException If an error occurs while (for instance) downloading the bundle or copying it to a
     * specific location on the filesystem.
     */
    @Throws(IOException::class)
    fun provideDistributionBundle(): ValkeyDistributionBundle

    /**
     * Chains this [ValkeyDistributionBundleProvider] with an [ExtractValkeyDistributionProvider] to provide a
     * [ValkeyDistribution] by first obtaining the bundle from this provider and then extracting it.
     *
     * @param installationPath The path where the Valkey distribution should be installed. If `null`, a default
     * temporary path is used.
     * @param ensureBinaryIsExecutable Whether to ensure that the Valkey binary is executable after extraction. Default
     * is `true`.
     *
     * @return A [ValkeyDistributionProvider] that first obtains the bundle from this provider and then extracts it.
     */
    fun thenExtract(
        installationPath: Path? = null,
        ensureBinaryIsExecutable: Boolean = true
    ): ValkeyDistributionProvider {
        return object : ValkeyDistributionProvider {
            override fun provideDistribution(): ValkeyDistribution {
                val bundle = provideDistributionBundle()
                return ExtractValkeyDistributionProvider(
                    valkeyDistributionBundle = bundle,
                    installationPath = (installationPath ?: resolveDefaultTempInstallationPath(
                        bundle.version,
                        bundle.operatingSystem
                    )),
                    ensureBinaryIsExecutable = ensureBinaryIsExecutable
                ).provideDistribution()
            }
        }
    }
}