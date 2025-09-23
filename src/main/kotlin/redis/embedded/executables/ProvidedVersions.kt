package redis.embedded.executables

import redis.embedded.model.Architecture
import redis.embedded.model.OsArchitecture
import redis.embedded.resource.DownloadLinuxDistroResourceSupplier
import redis.embedded.resource.DownloadMacOsDistroResourceSupplier
import redis.embedded.resource.DownloadWindowsDistroResourceSupplier
import redis.embedded.resource.ResourceSupplier

@JvmField
val PROVIDED_VERSIONS: Map<OsArchitecture, ResourceSupplier> = mapOf(
    OsArchitecture.UNIX_X86_64 to DownloadLinuxDistroResourceSupplier(Architecture.X86_64),
    OsArchitecture.UNIX_ARM64 to DownloadLinuxDistroResourceSupplier(Architecture.ARM64),
    OsArchitecture.WINDOWS_X86_64 to DownloadWindowsDistroResourceSupplier(Architecture.X86_64),
    OsArchitecture.MAC_OS_X_X86_64 to DownloadMacOsDistroResourceSupplier(Architecture.X86_64),
    OsArchitecture.MAC_OS_X_ARM64 to DownloadMacOsDistroResourceSupplier(Architecture.ARM64)
)