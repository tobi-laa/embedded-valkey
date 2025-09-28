package redis.embedded.resource

import io.github.tobi.laa.embedded.valkey.distribution.DEFAULT_MEMURAI_VERSION
import io.github.tobi.laa.embedded.valkey.distribution.downloadMemuraiDeveloperForX64FromNuget
import redis.embedded.model.Architecture
import java.nio.file.Path

// only temporary until code is refactored to rely on ValkeyDistributionProvider directly
class DownloadWindowsDistroResourceSupplier(val architecture: Architecture) : ResourceSupplier {

    override fun supplyResource(targetDirectory: Path): Path {
        val valkeyDistro = downloadMemuraiDeveloperForX64FromNuget(
            installationPath = targetDirectory
        ).provideDistribution()
        return valkeyDistro.binaryPath
    }

    override fun resourceName(): String {
        return "memurai-developer-$DEFAULT_MEMURAI_VERSION"
    }
}