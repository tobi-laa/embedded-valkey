package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallation
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSupplier
import io.github.tobi.laa.embedded.valkey.installation.ValkeyPackageExtractor
import io.github.tobi.laa.embedded.valkey.installation.resolveDefaultTempInstallationPath
import java.io.IOException
import java.nio.file.Path

/**
 * Supplies Valkey package, i.e., archives containing the Valkey server binary and potentially other files.
 */
interface ValkeyPackageSupplier {

    /**
     * Supplies a Valkey package. The exact steps to obtain the package are implementation-specific. This may involve
     * downloading an archive from the internet or locating it on the local filesystem or in the classpath.
     *
     * @return A [ValkeyPackage] containing information about the supplied package, including the path to the archive.
     *
     * @throws IOException If an error occurs while (for instance) downloading the package or copying it to a
     * specific location on the filesystem.
     */
    @Throws(IOException::class)
    fun retrievePackage(): ValkeyPackage

    /**
     * Chains this [ValkeyPackageSupplier] with an [ValkeyPackageExtractor] to supply a [ValkeyInstallation] by first
     * obtaining the package from this supplier and then extracting it.
     *
     * @param installationPath The path where the Valkey package should be installed. If `null`, a default temporary
     * path is used.
     * @param alwaysExtract Whether to always extract the Valkey package, even if it has apparently already been
     * extracted to the specified (or default) installation path. Default is `false`.
     * @param ensureBinaryIsExecutable Whether to ensure that the Valkey binary is executable after extraction. Default
     * is `true`.
     *
     * @return A [ValkeyInstallationSupplier] that first obtains the package from this supplier and then extracts it.
     */
    fun thenExtract(
        installationPath: Path? = null,
        alwaysExtract: Boolean = false,
        ensureBinaryIsExecutable: Boolean = true
    ): ValkeyInstallationSupplier {
        return ValkeyInstallationSupplier {
            val bundle = retrievePackage()
            ValkeyPackageExtractor(
                valkeyPackage = bundle,
                installationPath = (installationPath ?: resolveDefaultTempInstallationPath(
                    bundle.distributionType,
                    bundle.version,
                    bundle.operatingSystem
                )),
                ensureBinaryIsExecutable = ensureBinaryIsExecutable
            ).installValkey()
        }
    }
}