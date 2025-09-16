package redis.embedded.model

enum class OsArchitecture(val os: OS, val architecture: Architecture) {
    WINDOWS_X86_64(OS.WINDOWS, Architecture.X86_64),
    UNIX_X86_64(OS.UNIX, Architecture.X86_64),
    UNIX_ARM64(OS.UNIX, Architecture.AARCH64),
    MAC_OS_X_X86_64(OS.MAC_OS_X, Architecture.X86_64),
    MAC_OS_X_ARM64(OS.MAC_OS_X, Architecture.AARCH64);

    companion object {
        fun detectOSandArchitecture(): OsArchitecture {
            val os = OS.detectOS()
            val arch = os.detectArchitecture()
            return entries.firstOrNull { it.os == os && it.architecture == arch }
                ?: throw IllegalStateException("Unsupported OS/Architecture combination: $os/$arch")
        }
    }
}