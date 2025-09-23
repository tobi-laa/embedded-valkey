package redis.embedded.resource

import io.github.tobi.laa.embedded.valkey.distribution.DEFAULT_VALKEY_MAC_OS_VERSION
import io.github.tobi.laa.embedded.valkey.distribution.DownloadMacOsDistributionFromMacportsProvider
import redis.embedded.model.Architecture
import java.nio.file.Path

// only temporary until code is refactored to rely on ValkeyDistributionProvider directly
class DownloadMacOsDistroResourceSupplier(val architecture: Architecture) : ResourceSupplier {

    override fun supplyResource(targetDirectory: Path): Path {
        val valkeyDisto = DownloadMacOsDistributionFromMacportsProvider(
            architecture = architecture,
            installationPath = targetDirectory
        ).provideDistribution()
        return valkeyDisto.binaryPath
    }

    override fun resourceName(): String {
        return "valkey-server-$DEFAULT_VALKEY_MAC_OS_VERSION"
    }
}