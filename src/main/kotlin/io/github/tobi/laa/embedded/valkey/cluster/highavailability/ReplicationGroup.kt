package io.github.tobi.laa.embedded.valkey.cluster.highavailability

import redis.embedded.core.PortProvider
import java.util.stream.IntStream.range
import kotlin.streams.toList

data class ReplicationGroup(
    val mainNodeName: String,
    val mainNodePort: Int,
    val replicaPorts: List<Int>
) {

    constructor(mainNodeName: String, provider: PortProvider, replicaCount: Int) : this(
        mainNodeName,
        provider.get(),
        range(0, replicaCount).map { provider.get() }.toList()
    )
}
