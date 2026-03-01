package io.github.tobi.laa.embedded.valkey.operatingsystem

/**
 * Test-only extension of [OperatingSystem] that adds [HAIKU_OS_RISC_V] as an exotic, unsupported OS.
 * This shadows the production enum to enable tests that verify behaviour when no installation supplier
 * is available for the detected operating system.
 */
enum class OperatingSystem(val displayName: String) {
    LINUX_X86_64("Linux for x86_64"),
    LINUX_ARM64("Linux for ARM64"),
    MAC_OS_X86_64("Mac OS for x86_64"),
    MAC_OS_ARM64("Mac OS for ARM64"),
    WINDOWS_X86_64("Windows for x86_64"),
    HAIKU_OS_RISC_V("Haiku for RISC-V")
}
