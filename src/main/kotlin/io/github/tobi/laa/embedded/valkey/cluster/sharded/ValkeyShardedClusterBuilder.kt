package io.github.tobi.laa.embedded.valkey.cluster.sharded

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandaloneBuilder
import redis.embedded.core.PortProvider
import java.io.IOException
import java.time.Duration
import java.util.*

class ValkeyShardedClusterBuilder {

    private var serverBuilder = ValkeyStandaloneBuilder()
    private var shardPortProvider: PortProvider = PortProvider.newSequencePortProvider(6379)
    private var initializationTimeout: Duration = DEFAULT_INITIALIZATION_TIMEOUT
    private val shards: MutableList<Shard> = LinkedList<Shard>()
    private val replicasPortsByMainNodePort: MutableMap<Int, MutableSet<Int>> =
        LinkedHashMap<Int, MutableSet<Int>>()

    fun withServerBuilder(serverBuilder: ValkeyStandaloneBuilder): ValkeyShardedClusterBuilder {
        this.serverBuilder = serverBuilder
        return this
    }

    fun serverPorts(ports: Collection<Int>): ValkeyShardedClusterBuilder {
        this.shardPortProvider = PortProvider.newPredefinedPortProvider(ports)
        return this
    }

    fun initializationTimeout(initializationTimeout: Duration): ValkeyShardedClusterBuilder {
        this.initializationTimeout = initializationTimeout
        return this
    }

    fun ephemeralServers(): ValkeyShardedClusterBuilder {
        this.shardPortProvider = PortProvider.newEphemeralPortProviderInRedisClusterRange()
        return this
    }

    fun ephemeral(): ValkeyShardedClusterBuilder {
        ephemeralServers()
        return this
    }

    fun shard(name: String, replicaCount: Int): ValkeyShardedClusterBuilder {
        this.shards.add(Shard(name, this.shardPortProvider, replicaCount))
        return this
    }

    @Throws(IOException::class)
    fun build(): ValkeyShardedCluster {
        val servers = buildServers()
        return ValkeyShardedCluster(servers, replicasPortsByMainNodePort, initializationTimeout)
    }

    @Throws(IOException::class)
    private fun buildServers(): List<ValkeyStandalone> {
        val servers: MutableList<ValkeyStandalone> = ArrayList<ValkeyStandalone>()
        for (shard in shards) {
            servers.add(buildMainNode(shard))
            servers.addAll(buildReplicas(shard))
        }
        return servers
    }

    @Throws(IOException::class)
    private fun buildReplicas(shard: Shard): List<ValkeyStandalone> {
        val replicas: MutableList<ValkeyStandalone> = ArrayList<ValkeyStandalone>()
        val replicaPorts: MutableSet<Int> = replicasPortsByMainNodePort.get(shard.mainNodePort)!!
        for (replicaPort in shard.replicaPorts) {
            replicaPorts.add(replicaPort)
            val replicaBuilder = serverBuilder.clone()
            replicaBuilder.port(replicaPort)
            replicaBuilder.directive("cluster-enabled", "yes")
            replicaBuilder.directive("cluster-config-file", "nodes-replica-$replicaPort.conf")
            replicaBuilder.directive("cluster-node-timeout", "5000")
            replicaBuilder.directive("appendonly", "no")
            val slave = replicaBuilder.build()
            replicas.add(slave)
        }
        return replicas
    }

    @Throws(IOException::class)
    private fun buildMainNode(shard: Shard): ValkeyStandalone {
        replicasPortsByMainNodePort[shard.mainNodePort] = HashSet<Int>()
        val mainNodeBuilder = serverBuilder.clone()
        mainNodeBuilder.directive("cluster-enabled", "yes")
        mainNodeBuilder.directive("cluster-config-file", "nodes-main-" + shard.mainNodePort + ".conf")
        mainNodeBuilder.directive("cluster-node-timeout", "5000")
        mainNodeBuilder.directive("appendonly", "no")
        return mainNodeBuilder.port(shard.mainNodePort).build()
    }

    companion object {
        private val DEFAULT_INITIALIZATION_TIMEOUT: Duration = Duration.ofSeconds(20)
    }
}
