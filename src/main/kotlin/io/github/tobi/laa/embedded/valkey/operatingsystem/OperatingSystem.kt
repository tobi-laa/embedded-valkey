package io.github.tobi.laa.embedded.valkey.operatingsystem

import redis.embedded.model.Architecture
import redis.embedded.model.Architecture.ARM64
import redis.embedded.model.Architecture.X86_64

/**
 * Represents an operating system and its architecture.
 */
enum class OperatingSystem(val displayName: String, val architecture: Architecture) {
    LINUX_X86_64("Linux for x86_64", X86_64),
    LINUX_ARM64("Linux for ARM64", ARM64),
    MAC_OS_X86_64("Mac OS for x86_64", X86_64),
    MAC_OS_ARM64("Mac OS for ARM64", ARM64),
    WINDOWS_X86_64("Windows for x86_64", X86_64),
    WINDOWS_ARM64("Windows for ARM64", ARM64);
}