package redis.embedded.executables

import redis.embedded.model.OsArchitecture
import redis.embedded.resource.ResourceSupplier
import redis.embedded.resource.SimpleResourceSupplier
import redis.embedded.resource.TarGzipResourceSupplier

@JvmField
val PROVIDED_VERSIONS: Map<OsArchitecture, ResourceSupplier> = mapOf(
    OsArchitecture.UNIX_X86_64 to TarGzipResourceSupplier(
        "/valkey-8.1.3-jammy-x86_64.tar.gz",
        "valkey-8.1.3-jammy-x86_64/bin/valkey-server"
    ),
    OsArchitecture.UNIX_ARM64 to TarGzipResourceSupplier(
        "/valkey-8.1.3-jammy-arm64.tar.gz",
        "valkey-8.1.3-jammy-arm64/bin/valkey-server"
    ),
    OsArchitecture.WINDOWS_X86_64 to SimpleResourceSupplier("/redis-server-5.0.14.1-windows-amd64.exe"),
    OsArchitecture.MAC_OS_X_X86_64 to SimpleResourceSupplier("/redis-server-6.2.6-v5-darwin-amd64"),
    OsArchitecture.MAC_OS_X_ARM64 to SimpleResourceSupplier("/redis-server-6.2.6-v5-darwin-arm64")
)