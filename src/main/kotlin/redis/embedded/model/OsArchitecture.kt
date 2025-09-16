package redis.embedded.model

enum class OsArchitecture(val os: OS, val architecture: Architecture) {
    WINDOWS_x86_64(OS.WINDOWS, Architecture.x86_64),
    UNIX_x86_64(OS.UNIX, Architecture.x86_64),
    UNIX_AARCH64(OS.UNIX, Architecture.aarch64),
    MAC_OS_X_x86_64(OS.MAC_OS_X, Architecture.x86_64),
    MAC_OS_X_ARM64(OS.MAC_OS_X, Architecture.aarch64);

    companion object {
        fun detectOSandArchitecture(): OsArchitecture {
            val os = OS.detectOS()
            val arch = os.detectArchitecture()
            return entries.firstOrNull{ it.os == os && it.architecture == arch } ?: throw IllegalStateException("Unsupported OS/Architecture combination: $os/$arch")
        }
    }
}