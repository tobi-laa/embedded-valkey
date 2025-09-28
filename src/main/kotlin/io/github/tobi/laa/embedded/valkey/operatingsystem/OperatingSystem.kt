package io.github.tobi.laa.embedded.valkey.operatingsystem

/**
 * Represents an operating system and its architecture.
 */
enum class OperatingSystem(val displayName: String) {
    LINUX_X86_64("Linux for x86_64"),
    LINUX_ARM64("Linux for ARM64"),
    MAC_OS_X86_64("Mac OS for x86_64"),
    MAC_OS_ARM64("Mac OS for ARM64"),
    WINDOWS_X86_64("Windows for x86_64");
}