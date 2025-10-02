package io.github.tobi.laa.embedded.valkey.operatingsystem

import java.util.*

@Throws(UnsupportedOperatingSytemException::class)
fun detectOperatingSystem(): OperatingSystem {
    val operatingSystemName = operatingSystemName()
    return when {
        isWindows(operatingSystemName) -> detectWindowsOperatingSystem()
        isLinux(operatingSystemName) -> detectLinuxOperatingSystem()
        isMacOs(operatingSystemName) -> detectMacOsOperatingSystem()
        else -> throw UnsupportedOperatingSytemException("Unsupported operating system: $operatingSystemName")
    }
}

private fun operatingSystemName(): String {
    return System.getProperty("os.name")?.lowercase(Locale.getDefault())
        ?: throw OperatingSystemDetectionException("System property 'os.name' is not set")
}

private fun isWindows(operatingSystemName: String): Boolean {
    return operatingSystemName.contains("win")
}

private fun isLinux(operatingSystemName: String): Boolean {
    return operatingSystemName.contains("nix") || operatingSystemName.contains("nux") || operatingSystemName.contains("aix")
}

private fun isMacOs(operatingSystemName: String): Boolean {
    return "Mac OS X".equals(operatingSystemName, ignoreCase = true)
}


private fun detectWindowsOperatingSystem(): OperatingSystem {
    val arch = System.getenv("PROCESSOR_ARCHITECTURE")
    val wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432")
    if (arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64")) {
        return OperatingSystem.WINDOWS_X86_64
    } else {
        throw UnsupportedOperatingSytemException("Only 64-bit Windows is supported, but detected architecture is: $arch/$wow64Arch")
    }
}

private fun detectLinuxOperatingSystem(): OperatingSystem {
    val machineHardwareName = machineHardwareName()
    return if (machineHardwareName.contains("aarch64") || machineHardwareName.contains("arm64")) {
        OperatingSystem.LINUX_ARM64
    } else if (machineHardwareName.contains("x86_64") || machineHardwareName.contains("amd64")) {
        OperatingSystem.LINUX_X86_64
    } else {
        throw UnsupportedOperatingSytemException("Only x86_64 and ARM64 Linux is supported, but detected architecture is: $machineHardwareName")
    }
}

private fun detectMacOsOperatingSystem(): OperatingSystem {
    val machineHardwareName = machineHardwareName()
    return if (machineHardwareName.contains("aarch64") || machineHardwareName.contains("arm64")) {
        OperatingSystem.MAC_OS_ARM64
    } else if (machineHardwareName.contains("x86_64") || machineHardwareName.contains("amd64")) {
        OperatingSystem.MAC_OS_X86_64
    } else {
        throw UnsupportedOperatingSytemException("Only x86_64 and ARM64 Linux is supported, but detected architecture is: $machineHardwareName")
    }
}

@Suppress("kotlin:S117") // the variable name "uname -m" seems more readable here
private fun machineHardwareName(): String {
    try {
        val `uname -m` = ProcessBuilder("uname", "-m").start()
        `uname -m`.inputReader().use {
            val machineHardwareNames = it.lines().toList()
            if (machineHardwareNames.isEmpty()) {
                throw OperatingSystemDetectionException("Cannot determine machine hardware name. 'uname -m' returned no output.")
            } else if (machineHardwareNames.size > 1) {
                throw OperatingSystemDetectionException("Cannot determine machine hardware name. 'uname -m' returned multiple lines: $machineHardwareNames")
            } else {
                return machineHardwareNames[0]
            }
        }
    } catch (e: Exception) {
        throw OperatingSystemDetectionException("Cannot determine machine hardware name.", e)
    }
}