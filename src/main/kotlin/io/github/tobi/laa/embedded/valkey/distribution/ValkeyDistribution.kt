package io.github.tobi.laa.embedded.valkey.distribution

import redis.embedded.model.Architecture
import redis.embedded.model.OS
import java.nio.file.Path

data class ValkeyDistribution(
    val version: String,
    val operatingSystem: OS,
    val architecture: Architecture,
    val installationPath: Path,
    val binaryPath: Path
)
