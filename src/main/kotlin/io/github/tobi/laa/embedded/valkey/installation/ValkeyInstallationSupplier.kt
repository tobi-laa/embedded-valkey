package io.github.tobi.laa.embedded.valkey.installation

import java.io.IOException

/**
 * Provides Valkey distributions, i.e., directories containing Valkey installations with the Valkey server binary and
 * potentially other files along with metadata about the installation, such as version, operating system,
 * and architecture.
 */
@FunctionalInterface
interface ValkeyInstallationSupplier {

    /**
     * Provides a Valkey distribution. The exact steps to obtain the distribution are implementation-specific. This may
     * involve downloading an archive from the internet, extracting it, and locating the Valkey server binary within
     * the extracted files.
     *
     * @return A [ValkeyInstallation] containing information about the provided distribution, including the path to the
     * Valkey server binary.
     *
     * @throws java.io.IOException If an error occurs while (for instance) extracting a (previously downloaded) archive
     * or copying files to a specific location on the filesystem.
     */
    @Throws(IOException::class)
    fun installValkey(): ValkeyInstallation
}