package io.github.tobi.laa.embedded.valkey.installation

import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.LINUX_ARM64
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.LINUX_X86_64
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.MAC_OS_ARM64
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.MAC_OS_X86_64
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.WINDOWS_X86_64
import io.github.tobi.laa.embedded.valkey.valkeypackage.DEFAULT_MACPORTS_BUILD_FILE_PATHS
import io.github.tobi.laa.embedded.valkey.valkeypackage.DEFAULT_MACPORTS_CHECKSUMS
import io.github.tobi.laa.embedded.valkey.valkeypackage.DEFAULT_MEMURAI_VERSION
import io.github.tobi.laa.embedded.valkey.valkeypackage.DEFAULT_VALKEY_LINUX_VERSION
import io.github.tobi.laa.embedded.valkey.valkeypackage.DEFAULT_VALKEY_MAC_OS_VERSION
import io.github.tobi.laa.embedded.valkey.valkeypackage.NUGET_FILE_CHECKSUMS
import io.github.tobi.laa.embedded.valkey.valkeypackage.VALKEY_IO_FILE_CHECKSUMS
import io.github.tobi.laa.embedded.valkey.valkeypackage.downloadLinuxPackageFromValkeyIo
import io.github.tobi.laa.embedded.valkey.valkeypackage.downloadMacOsPackageFromMacPorts
import io.github.tobi.laa.embedded.valkey.valkeypackage.downloadWinX64MemuraiPackageFromNuget
import java.net.Proxy
import java.nio.file.Path

/**
 * Returns a [ValkeyInstallationSupplier] which downloads and installs Valkey for Linux from [valkey.io](https://valkey.io).
 *
 * @param proxy The proxy to use for downloading the package (defaults to not using a proxy).
 * @param operatingSystem The operating system for which the Valkey package is built. Defaults to [LINUX_X86_64]. *Must* be a Linux operating system.
 * @param installationPath The path where the Valkey distribution should be installed. If `null`, a temporary directory will be used.
 * @param valkeyVersion The Valkey version to download. Defaults to [DEFAULT_VALKEY_LINUX_VERSION].
 * @param sha256FileChecksum The expected SHA-256 checksum of the downloaded package for integrity verification. If `null`, no integrity verification will be performed.
 * If a checksum is available for the specified version and operating system, it will be used by default.
 * You can look up checksums for other versions and operating systems on the [Valkey download page](https://valkey.io/download/).
 *
 * @return A [ValkeyInstallationSupplier] that downloads and installs the specified Valkey version for Linux.
 */
@JvmOverloads
fun downloadAndInstallLinuxPackageFromValkeyIo(
    proxy: Proxy? = null,
    operatingSystem: OperatingSystem = LINUX_X86_64,
    installationPath: Path? = null,
    valkeyVersion: String = DEFAULT_VALKEY_LINUX_VERSION,
    sha256FileChecksum: String? = VALKEY_IO_FILE_CHECKSUMS[Pair(valkeyVersion, operatingSystem)]
): ValkeyInstallationSupplier {
    return downloadLinuxPackageFromValkeyIo(
        proxy = proxy,
        operatingSystem = operatingSystem,
        valkeyVersion = valkeyVersion,
        sha256FileChecksum = sha256FileChecksum
    ).thenExtract(installationPath)
}

/**
 * Returns a [ValkeyInstallationSupplier] which downloads and installs Valkey for macOS from [MacPorts](https://www.macports.org/).
 *
 * @param proxy The proxy to use for downloading the package (defaults to not using a proxy).
 * @param operatingSystem The operating system for which the Valkey package is built. Defaults to [MAC_OS_X86_64]. *Must* be a macOS operating system.
 * @param installationPath The path where the Valkey package should be installed. If `null`, a temporary directory will be used.
 * @param valkeyVersion The Valkey version to download. Defaults to [DEFAULT_VALKEY_MAC_OS_VERSION].
 * @param buildFilePath The build file path within the MacPorts package repository. This *must* be specified if a (non-default) Valkey version should be downloaded.
 * A build file path can be looked up in the [MacPorts package repository](https://packages.macports.org/valkey/) and has a format like `valkey-8.1.3_0.darwin_24.x86_64.tbz2`.
 * @param sha256FileChecksum The expected SHA-256 checksum of the downloaded bundle for integrity verification. If `null`, no integrity verification will be performed.
 * If a checksum is available for the specified version and operating system, it will be used by default.
 * As MacPorts does not publish SHA-256 checksums, you will have to compute them manually for other versions and operating systems.
 * @return A [ValkeyInstallationSupplier] that downloads and installs the specified Valkey distro for macOS.
 */
@JvmOverloads
fun downloadAndInstallMacOsPackageFromMacports(
    proxy: Proxy? = null,
    operatingSystem: OperatingSystem = MAC_OS_X86_64,
    installationPath: Path? = null,
    valkeyVersion: String = DEFAULT_VALKEY_MAC_OS_VERSION,
    buildFilePath: String = DEFAULT_MACPORTS_BUILD_FILE_PATHS[Pair(valkeyVersion, operatingSystem)]
        ?: throw IllegalArgumentException("No MacPorts build file path found for Valkey version $valkeyVersion and operating system ${operatingSystem.displayName}."),
    sha256FileChecksum: String? = DEFAULT_MACPORTS_CHECKSUMS[buildFilePath]
): ValkeyInstallationSupplier {
    return downloadMacOsPackageFromMacPorts(
        proxy = proxy,
        operatingSystem = operatingSystem,
        valkeyVersion = valkeyVersion,
        buildFilePath = buildFilePath,
        sha256FileChecksum = sha256FileChecksum
    ).thenExtract(installationPath)
}

/**
 * Returns a [ValkeyInstallationSupplier] which downloads the Memurai Developer Edition for Windows x64 from
 * [NuGet](https://www.nuget.org/).
 *
 * @param proxy The proxy to use for downloading the bundle (defaults to not using a proxy).
 * @param installationPath The path where the Memurai distribution should be installed. If `null`, a temporary directory will be used.
 * @param memuraiVersion The Memurai version to download. Defaults to [DEFAULT_MEMURAI_VERSION].
 * @param sha256FileChecksum The expected SHA-256 checksum of the downloaded bundle for integrity verification. If `null`, no integrity verification will be performed.
 * If a checksum is available for the specified version, it will be used by default.
 * As NuGet does not publish SHA-256 checksums, you will have to compute them manually for other versions.
 * @return A [ValkeyInstallationSupplier] that downloads and installs the specified Memurai Developer Edition for Windows x64.
 */
@JvmOverloads
fun downloadAndInstallMemuraiDeveloperForX64FromNuget(
    memuraiVersion: String = DEFAULT_MEMURAI_VERSION,
    proxy: Proxy? = null,
    sha256FileChecksum: String? = NUGET_FILE_CHECKSUMS[memuraiVersion],
    installationPath: Path? = null
): ValkeyInstallationSupplier {
    return downloadWinX64MemuraiPackageFromNuget(
        proxy = proxy,
        memuraiVersion = memuraiVersion,
        sha256FileChecksum = sha256FileChecksum
    ).thenExtract(installationPath)
}

/**
 * The default Valkey distribution provided by this library for each supported operating system.
 * The default distributions are:
 * - Linux x86_64: Valkey version [DEFAULT_VALKEY_LINUX_VERSION] from [valkey.io](https://valkey.io)
 * - Linux arm64: Valkey version [DEFAULT_VALKEY_LINUX_VERSION] from [valkey.io](https://valkey.io)
 * - Windows x86_64: Memurai Developer Edition version [DEFAULT_MEMURAI_VERSION] from [NuGet](https://www.nuget.org/)
 * - macOS x86_64: Valkey version [DEFAULT_VALKEY_MAC_OS_VERSION] from [MacPorts](https://www.macports.org/)
 * - macOS arm64: Valkey version [DEFAULT_VALKEY_MAC_OS_VERSION] from [MacPorts](https://www.macports.org/)
 */
@JvmField
val DEFAULT_PROVIDERS: Map<OperatingSystem, ValkeyInstallationSupplier> = mapOf(
    LINUX_X86_64 to downloadAndInstallLinuxPackageFromValkeyIo(operatingSystem = LINUX_X86_64),
    LINUX_ARM64 to downloadAndInstallLinuxPackageFromValkeyIo(operatingSystem = LINUX_ARM64),
    WINDOWS_X86_64 to downloadAndInstallMemuraiDeveloperForX64FromNuget(),
    MAC_OS_X86_64 to downloadAndInstallMacOsPackageFromMacports(operatingSystem = MAC_OS_X86_64),
    MAC_OS_ARM64 to downloadAndInstallMacOsPackageFromMacports(operatingSystem = MAC_OS_ARM64),
)