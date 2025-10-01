package io.github.tobi.laa.embedded.valkey.distribution.bundle

import io.github.tobi.laa.embedded.valkey.distribution.DistributionType
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.LINUX_ARM64
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.LINUX_X86_64
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.MAC_OS_ARM64
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.MAC_OS_X86_64
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.WINDOWS_X86_64
import org.slf4j.LoggerFactory.getLogger
import java.net.Proxy
import java.net.URI
import java.nio.file.Paths

private val log = getLogger("io.github.tobi.laa.embedded.valkey.distribution.bundle")

@JvmField
val DEFAULT_VALKEY_LINUX_VERSION = "8.1.3"

// SHA-256 file checksums can be retrieved from https://valkey.io/download/ per release
internal val VALKEY_IO_FILE_CHECKSUMS =
    mapOf(
        Pair(
            DEFAULT_VALKEY_LINUX_VERSION,
            LINUX_X86_64,
        ) to "afbbf9e71f171472679280f9e06a8039cf3f1ac7ab6dff76ef3094852c7a342f",
        Pair(
            DEFAULT_VALKEY_LINUX_VERSION,
            LINUX_ARM64,
        ) to "a5f9fc1ed32f8ec56c15665b7edd6fbd4702e0947b1a7eb9f604062ac0c720d0"
    )

/**
 * Returns a [ValkeyDistributionBundleProvider] which downloads the Valkey distribution for Linux from [valkey.io](https://valkey.io).
 *
 * @param proxy The proxy to use for downloading the bundle (defaults to not using a proxy).
 * @param operatingSystem The operating system of the Valkey distribution to download. Defaults to [LINUX_X86_64].
 * @param valkeyVersion The Valkey version to download. Defaults to [DEFAULT_VALKEY_LINUX_VERSION].
 * @param sha256FileChecksum The expected SHA-256 checksum of the downloaded bundle for integrity verification. If `null`, no integrity verification will be performed.
 * If a checksum is available for the specified version and operating system, it will be used by default.
 * You can look up checksums for other versions and operating systems on the [Valkey download page](https://valkey.io/download/).
 *
 * @return A [ValkeyDistributionBundleProvider] that downloads specified Valkey distro for Linux.
 */
@JvmOverloads
fun downloadLinuxDistroFromValkeyIo(
    proxy: Proxy = Proxy.NO_PROXY,
    operatingSystem: OperatingSystem = LINUX_X86_64,
    valkeyVersion: String = DEFAULT_VALKEY_LINUX_VERSION,
    sha256FileChecksum: String? = VALKEY_IO_FILE_CHECKSUMS[Pair(valkeyVersion, operatingSystem)]
): ValkeyDistributionBundleProvider {
    require(operatingSystem == LINUX_X86_64 || operatingSystem == LINUX_ARM64) {
        "Operating system must be either $LINUX_X86_64 or $LINUX_ARM64."
    }
    val arch = if (operatingSystem == LINUX_X86_64) {
        "x86_64"
    } else {
        "arm64"
    }
    if (sha256FileChecksum == null) {
        logWarnNoChecksum(valkeyVersion, operatingSystem)
    }
    return DownloadValkeyDistroBundleProvider(
        valkeyVersion = valkeyVersion,
        operatingSystem = operatingSystem,
        binaryPathWithinBundle = Paths.get("valkey-$valkeyVersion-jammy-$arch", "bin", "valkey-server"),
        archiveType = ArchiveType.TAR_GZ,
        downloadUri = URI("https://download.valkey.io/releases/valkey-$valkeyVersion-jammy-$arch.tar.gz"),
        proxy = proxy,
        sha256FileChecksum = sha256FileChecksum
    )
}

@JvmField
val DEFAULT_VALKEY_MAC_OS_VERSION = "8.1.3"

// see https://packages.macports.com/valkey/ for possible build file paths
internal val DEFAULT_MACPORTS_BUILD_FILE_PATHS =
    mapOf(
        Pair(
            DEFAULT_VALKEY_MAC_OS_VERSION,
            MAC_OS_X86_64
        ) to "valkey-${DEFAULT_VALKEY_MAC_OS_VERSION}_0.darwin_24.x86_64.tbz2",
        Pair(
            DEFAULT_VALKEY_MAC_OS_VERSION,
            MAC_OS_ARM64
        ) to "valkey-${DEFAULT_VALKEY_MAC_OS_VERSION}_0.darwin_25.arm64.tbz2"
    )

// MacPorts does not publish SHA-256 checksums, so these have been computed manually
internal val DEFAULT_MACPORTS_CHECKSUMS =
    mapOf(
        DEFAULT_MACPORTS_BUILD_FILE_PATHS[Pair(
            DEFAULT_VALKEY_MAC_OS_VERSION,
            MAC_OS_X86_64
        )]!! to "0ed1125217309f41220aa47a7e1b6ad421c51bba890c80c0086f3a3c634e4231",
        DEFAULT_MACPORTS_BUILD_FILE_PATHS[Pair(
            DEFAULT_VALKEY_MAC_OS_VERSION,
            MAC_OS_ARM64
        )]!! to "b17b01d144df854c5cdbaea7b56c24718e99a18d8b00828b7ea2568a0df44a14"
    )

/**
 * Returns a [ValkeyDistributionBundleProvider] which downloads the Valkey distribution for macOS from
 * [MacPorts](https://www.macports.org/).
 *
 * @param proxy The proxy to use for downloading the bundle (defaults to not using a proxy).
 * @param operatingSystem The operating system of the Valkey distribution to download. Must be either [MAC_OS_X86_64] or [MAC_OS_ARM64].
 * @param valkeyVersion The Valkey version to download. Defaults to [DEFAULT_VALKEY_MAC_OS_VERSION].
 * @param buildFilePath The build file path within the MacPorts package repository. This *must* be specified if a (non-default) Valkey version should be downloaded.
 * A build file path can be looked up in the [MacPorts package repository](https://packages.macports.org/valkey/) and has a format like `valkey-8.1.3_0.darwin_24.x86_64.tbz2`.
 * @param sha256FileChecksum The expected SHA-256 checksum of the downloaded bundle for integrity verification. If `null`, no integrity verification will be performed.
 * If a checksum is available for the specified version and operating system, it will be used by default.
 * As MacPorts does not publish SHA-256 checksums, you will have to compute them manually for other versions and operating systems.
 * @return A [ValkeyDistributionBundleProvider] that downloads the specified Valkey distro for macOS.
 */
@JvmOverloads
fun downloadMacOsDistroFromMacports(
    proxy: Proxy = Proxy.NO_PROXY,
    operatingSystem: OperatingSystem = MAC_OS_X86_64,
    valkeyVersion: String = DEFAULT_VALKEY_MAC_OS_VERSION,
    buildFilePath: String = DEFAULT_MACPORTS_BUILD_FILE_PATHS[Pair(valkeyVersion, operatingSystem)]
        ?: throw IllegalArgumentException("No MacPorts build file path found for Valkey version $valkeyVersion and operating system ${operatingSystem.displayName}."),
    sha256FileChecksum: String? = DEFAULT_MACPORTS_CHECKSUMS[buildFilePath]
): ValkeyDistributionBundleProvider {
    require(operatingSystem == MAC_OS_X86_64 || operatingSystem == MAC_OS_ARM64) {
        "Operating system must be either $MAC_OS_X86_64 or $MAC_OS_ARM64."
    }
    if (sha256FileChecksum == null) {
        logWarnNoChecksum(valkeyVersion, operatingSystem)
    }
    return DownloadValkeyDistroBundleProvider(
        valkeyVersion = valkeyVersion,
        operatingSystem = operatingSystem,
        binaryPathWithinBundle = Paths.get("opt", "local", "bin", "valkey-server"),
        archiveType = ArchiveType.TAR_BZ2,
        downloadUri = URI("https://packages.macports.com/valkey/$buildFilePath"),
        proxy = proxy,
        sha256FileChecksum = sha256FileChecksum
    )
}

@JvmField
val DEFAULT_MEMURAI_VERSION = "4.1.6"

// SHA-256 file checksums are not published on NuGet, so these have been computed manually
internal val NUGET_FILE_CHECKSUMS = mapOf(
    DEFAULT_MEMURAI_VERSION to "768cfe17324111a7ad18b4190879dded8d7531fb85b22a006e8e2b3aca4f0a4c"
)

/**
 * Returns a [ValkeyDistributionBundleProvider] which downloads the Memurai Developer Edition for Windows x64 from
 * [NuGet](https://www.nuget.org/).
 *
 * @param proxy The proxy to use for downloading the bundle (defaults to not using a proxy).
 * @param memuraiVersion The Memurai version to download. Defaults to [DEFAULT_MEMURAI_VERSION].
 * @param sha256FileChecksum The expected SHA-256 checksum of the downloaded bundle for integrity verification. If `null`, no integrity verification will be performed.
 * If a checksum is available for the specified version, it will be used by default.
 * As NuGet does not publish SHA-256 checksums, you will have to compute them manually for other versions.
 * @return A [ValkeyDistributionBundleProvider] that downloads the specified Memurai Developer Edition for Windows x64.
 */
@JvmOverloads
fun downloadMemuraiDeveloperForX64FromNuget(
    proxy: Proxy = Proxy.NO_PROXY,
    memuraiVersion: String = DEFAULT_MEMURAI_VERSION,
    sha256FileChecksum: String? = NUGET_FILE_CHECKSUMS[memuraiVersion]
): ValkeyDistributionBundleProvider {
    if (sha256FileChecksum == null) {
        log.warn("No SHA-256 checksum present for Memurai Developer version $memuraiVersion. File integrity will not be verified!")
    }
    return DownloadValkeyDistroBundleProvider(
        valkeyVersion = memuraiVersion,
        operatingSystem = WINDOWS_X86_64,
        distributionType = DistributionType.MEMURAI,
        binaryPathWithinBundle = Paths.get("tools", "memurai.exe"),
        archiveType = ArchiveType.ZIP,
        downloadUri = URI("https://www.nuget.org/api/v2/package/MemuraiDeveloper/$memuraiVersion"),
        proxy = proxy,
        sha256FileChecksum = sha256FileChecksum
    )
}

private fun logWarnNoChecksum(valkeyVersion: String, operatingSystem: OperatingSystem) {
    log.warn("No SHA-256 checksum present for Valkey version $valkeyVersion and operating system ${operatingSystem.displayName}. File integrity will not be verified!")
}