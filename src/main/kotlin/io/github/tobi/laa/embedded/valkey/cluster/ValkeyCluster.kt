package io.github.tobi.laa.embedded.valkey.cluster

import io.github.tobi.laa.embedded.valkey.Valkey
import io.github.tobi.laa.embedded.valkey.ValkeyNode

/**
 * Represents a Valkey cluster, i.e. a collection of interconnected Valkey nodes.
 */
interface ValkeyCluster : Valkey {

    /**
     * The nodes that are part of this Valkey cluster.
     */
    val nodes: List<ValkeyNode>
}