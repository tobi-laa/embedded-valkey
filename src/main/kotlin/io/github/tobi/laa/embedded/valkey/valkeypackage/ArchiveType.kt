package io.github.tobi.laa.embedded.valkey.valkeypackage

/**
 * Represents the archive type of [ValkeyPackage].
 */
enum class ArchiveType(val fileExtension: String) {
    TAR_GZ("tar.gz"),
    TAR_BZ2("tar.bz2"),
    ZIP("zip")
}