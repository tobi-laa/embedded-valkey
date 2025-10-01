package io.github.tobi.laa.embedded.valkey.cluster.sharded

import redis.embedded.core.PortProvider
import java.util.stream.IntStream.range
import kotlin.streams.toList

data class Shard(val name: String, val mainNodePort: Int, val replicaPorts: List<Int>) {

    constructor(name: String, provider: PortProvider, replicaCount: Int) : this(
        name,
        provider.get(),
        range(0, replicaCount).map { provider.get() }.toList()
    )
}
