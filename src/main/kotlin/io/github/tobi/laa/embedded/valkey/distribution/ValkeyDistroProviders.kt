package io.github.tobi.laa.embedded.valkey.distribution

import io.github.tobi.laa.embedded.valkey.distribution.bundle.ArchiveType
import io.github.tobi.laa.embedded.valkey.distribution.bundle.DownloadValkeyDistroBundleProvider
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.*
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

const val DEFAULT_VALKEY_LINUX_VERSION = "8.1.3"

/**
 * Returns a [ValkeyDistributionProvider] which downloads the Valkey distribution for Linux from [valkey.io](https://valkey.io).
 *
 * @param valkeyVersion The Valkey version to download. Defaults to [DEFAULT_VALKEY_LINUX_VERSION].
 * @param operatingSystem The operating system of the Valkey distribution to download. Defaults to [LINUX_X86_64].
 * @param installationPath The path where the Valkey distribution should be installed. If `null`, a temporary directory will be used.
 *
 * @return A [ValkeyDistributionProvider] that downloads and installs the specified Valkey distro for Linux.
 */
fun downloadLinuxDistroFromValkeyIo(
    valkeyVersion: String = DEFAULT_VALKEY_LINUX_VERSION,
    operatingSystem: OperatingSystem = LINUX_X86_64,
    installationPath: Path? = null
): ValkeyDistributionProvider {
    require(operatingSystem == LINUX_X86_64 || operatingSystem == LINUX_ARM64) {
        "Operating system must be either $LINUX_X86_64 or $LINUX_ARM64."
    }
    val arch = if (operatingSystem == LINUX_X86_64) {
        "x86_64"
    } else {
        "arm64"
    }
    return DownloadValkeyDistroBundleProvider(
        valkeyVersion = valkeyVersion,
        operatingSystem = operatingSystem,
        binaryPathWithinBundle = Paths.get("valkey-$valkeyVersion-jammy-$arch", "bin", "valkey-server"),
        archiveType = ArchiveType.TAR_GZ,
        downloadUri = URI("https://download.valkey.io/releases/valkey-$valkeyVersion-jammy-$arch.tar.gz")
    ).thenExtract(installationPath)
}

const val DEFAULT_VALKEY_MAC_OS_VERSION = "8.1.3"
internal val DEFAULT_MAC_OS_BUILD_FILE_PATHS =
    mapOf(
        MAC_OS_X86_64 to "valkey-${DEFAULT_VALKEY_MAC_OS_VERSION}_0.darwin_24.x86_64.tbz2",
        MAC_OS_ARM64 to "valkey-${DEFAULT_VALKEY_MAC_OS_VERSION}_0.darwin_25.arm64.tbz2"
    )

/**
 * Returns a [ValkeyDistributionProvider] which downloads the Valkey distribution for macOS from
 * [MacPorts](https://www.macports.org/).
 *
 * @param valkeyVersion The Valkey version to download. Defaults to [DEFAULT_VALKEY_MAC_OS_VERSION].
 * @param architecture The architecture of the Valkey distribution to download. Defaults to [X86_64].
 * @param installationPath The path where the Valkey distribution should be installed. If `null`, a temporary directory will be used.
 * @return A [ValkeyDistributionProvider] that downloads and installs the specified Valkey distro for macOS.
 */
fun downloadMacOsDistroFromMacports(
    valkeyVersion: String = DEFAULT_VALKEY_MAC_OS_VERSION,
    operatingSystem: OperatingSystem = MAC_OS_X86_64,
    installationPath: Path? = null
): ValkeyDistributionProvider {
    require(operatingSystem == MAC_OS_X86_64 || operatingSystem == MAC_OS_ARM64) {
        "Operating system must be either $MAC_OS_X86_64 or $MAC_OS_ARM64."
    }
    val buildFilePath = DEFAULT_MAC_OS_BUILD_FILE_PATHS[operatingSystem]!!
    return DownloadValkeyDistroBundleProvider(
        valkeyVersion = valkeyVersion,
        operatingSystem = operatingSystem,
        binaryPathWithinBundle = Paths.get("opt", "local", "bin", "valkey-server"),
        archiveType = ArchiveType.TAR_BZ2,
        downloadUri = URI("https://packages.macports.com/valkey/$buildFilePath")
    ).thenExtract(installationPath)
}

const val DEFAULT_MEMURAI_VERSION = "4.1.6"

/**
 * Returns a [ValkeyDistributionProvider] which downloads the Memurai Developer Edition for Windows x64 from
 * [NuGet](https://www.nuget.org/).
 *
 * @param memuraiVersion The Memurai version to download. Defaults to [DEFAULT_MEMURAI_VERSION].
 * @param installationPath The path where the Memurai distribution should be installed. If `null`, a temporary directory will be used.
 * @return A [ValkeyDistributionProvider] that downloads and installs the specified Memurai Developer Edition for Windows x64.
 */
fun downloadMemuraiDeveloperForX64FromNuget(
    memuraiVersion: String = DEFAULT_MEMURAI_VERSION,
    installationPath: Path? = null
): ValkeyDistributionProvider {
    return DownloadValkeyDistroBundleProvider(
        valkeyVersion = memuraiVersion,
        operatingSystem = WINDOWS_X86_64,
        distributionType = DistributionType.MEMURAI,
        binaryPathWithinBundle = Paths.get("tools", "memurai.exe"),
        archiveType = ArchiveType.ZIP,
        downloadUri = URI("https://www.nuget.org/api/v2/package/MemuraiDeveloper/$memuraiVersion")
    ).thenExtract(installationPath)
}

@JvmField
val DEFAULT_PROVIDERS: Map<OperatingSystem, ValkeyDistributionProvider> = mapOf(
    LINUX_X86_64 to downloadLinuxDistroFromValkeyIo(operatingSystem = LINUX_X86_64),
    LINUX_ARM64 to downloadLinuxDistroFromValkeyIo(operatingSystem = LINUX_ARM64),
    WINDOWS_X86_64 to downloadMemuraiDeveloperForX64FromNuget(),
    MAC_OS_X86_64 to downloadMacOsDistroFromMacports(operatingSystem = MAC_OS_X86_64),
    MAC_OS_ARM64 to downloadMacOsDistroFromMacports(operatingSystem = MAC_OS_ARM64),
)