package io.github.tobi.laa.embedded.valkey.cluster.sharded

class ValkeyShardedClusterSetupException : RuntimeException {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
