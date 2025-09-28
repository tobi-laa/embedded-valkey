package io.github.tobi.laa.embedded.valkey.distribution.bundle

/**
 * Represents the archive type of [ValkeyDistributionBundle].
 */
enum class ArchiveType(val fileExtension: String) {
    TAR_GZ("tar.gz"),
    TAR_BZ2("tar.bz2"),
    ZIP("zip")
}