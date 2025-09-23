package redis.embedded.resource

import io.github.tobi.laa.embedded.valkey.distribution.DEFAULT_MEMURAI_VERSION
import io.github.tobi.laa.embedded.valkey.distribution.DownloadMemuraiDistributionFromNugetProvider
import redis.embedded.model.Architecture
import java.nio.file.Path

// only temporary until code is refactored to rely on ValkeyDistributionProvider directly
class DownloadWindowsDistroResourceSupplier(val architecture: Architecture) : ResourceSupplier {

    override fun supplyResource(targetDirectory: Path): Path {
        val valkeyDisto = DownloadMemuraiDistributionFromNugetProvider(
            architecture = architecture,
            installationPath = targetDirectory
        ).provideDistribution()
        return valkeyDisto.binaryPath
    }

    override fun resourceName(): String {
        return "memurai-developer-$DEFAULT_MEMURAI_VERSION"
    }
}