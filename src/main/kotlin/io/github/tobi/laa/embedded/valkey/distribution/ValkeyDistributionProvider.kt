package io.github.tobi.laa.embedded.valkey.distribution

/*
 * Provides Valkey distributions, i.e., archives containing the Valkey server binary and potentially other files.
 */
interface ValkeyDistributionProvider {

    /**
     * Provides a Valkey distribution. The exact steps to obtain the distribution are implementation-specific. This may
     * involve downloading an archive from the internet, extracting it, and locating the Valkey server binary within
     * the extracted files.
     *
     * @return A [ValkeyDistribution] containing information about the provided distribution, including the path to the
     * Valkey server binary.
     */
    fun provideDistribution(): ValkeyDistribution
}