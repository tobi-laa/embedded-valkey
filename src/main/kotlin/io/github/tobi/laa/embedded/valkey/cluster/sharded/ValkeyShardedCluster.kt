package io.github.tobi.laa.embedded.valkey.cluster.sharded

import io.github.tobi.laa.embedded.valkey.cluster.ValkeyCluster
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.args.ClusterResetType
import java.io.IOException
import java.time.Duration
import java.util.function.Supplier
import java.util.stream.IntStream

private const val CLUSTER_IP = "127.0.0.1"
private const val MAX_NUMBER_OF_SLOTS_PER_CLUSTER = 16384
private val SLEEP_DURATION: Duration = Duration.ofMillis(300)
private val SLEEP_DURATION_IN_MILLIS: Long = SLEEP_DURATION.toMillis()

class ValkeyShardedCluster(
    override val nodes: List<ValkeyStandalone>,
    private val replicasPortsByMainNodePort: Map<Int, MutableSet<Int>>,
    private val initializationTimeout: Duration
) : ValkeyCluster {

    private val log: Logger = LoggerFactory.getLogger(ValkeyShardedCluster::class.java)

    private val mainNodeIdsByPort: MutableMap<Int, String> = LinkedHashMap<Int, String>()

    @Throws(IOException::class)
    override fun start(awaitReadiness: Boolean, maxWaitTimeSeconds: Long) {
        for (node in nodes) {
            node.start(awaitReadiness, maxWaitTimeSeconds)
        }
        linkReplicasAndShards()
    }

    @Throws(IOException::class)
    override fun stop(forcibly: Boolean, maxWaitTimeSeconds: Long, removeWorkingDir: Boolean) {
        val exceptions: MutableList<Exception> = ArrayList<Exception>()
        exceptions.addAll(safelyPerformFlushAllAndSoftClusterResetOnMainNodes())
        for (node in nodes) {
            stopSafely(node, forcibly, maxWaitTimeSeconds, removeWorkingDir)?.let(exceptions::add)
        }
        if (!exceptions.isEmpty()) {
            throw IOException("Failed to stop Redis cluster", exceptions.get(0))
        }
    }

    private fun safelyPerformFlushAllAndSoftClusterResetOnMainNodes(): List<Exception> {
        val errors: MutableList<Exception> = ArrayList()
        for (mainNodePort in replicasPortsByMainNodePort.keys) {
            try {
                Jedis(CLUSTER_IP, mainNodePort, 10000).use { jedis ->
                    jedis.flushAll()
                    jedis.clusterReset(ClusterResetType.SOFT)
                }
            } catch (e: RuntimeException) {
                log.error("Failed to flush main node at port: {}", mainNodePort, e)
                errors.add(e)
            }
        }
        return errors
    }

    private fun stopSafely(
        node: ValkeyStandalone,
        forcibly: Boolean,
        maxWaitTimeSeconds: Long,
        removeWorkingDir: Boolean
    ): Exception? {
        try {
            node.stop(forcibly, maxWaitTimeSeconds, removeWorkingDir)
            return null
        } catch (e: IOException) {
            log.error("Failed to stop Redis instance", e)
            return e
        } catch (e: RuntimeException) {
            log.error("Failed to stop Redis instance", e)
            return e
        }
    }

    fun serverPorts(): List<Int> {
        return nodes.map { it.port }.toList()
    }

    private fun linkReplicasAndShards() {
        try {
            val mainNodePorts = replicasPortsByMainNodePort.keys
            // Use the first node as the target to be met by all other nodes
            val clusterMeetTarget = mainNodePorts.iterator().next()
            meetMainNodes(clusterMeetTarget)
            setupReplicas(clusterMeetTarget)
            waitForClusterToBeInteractReady()
        } catch (e: ValkeyShardedClusterSetupException) {
            try {
                this.stop()
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }
            throw RuntimeException(e)
        }
    }

    private fun meetMainNodes(clusterMeetTarget: Int) {
        // for every shard meet the main node (except the 1st shard) and add their slots manually
        val shardsMainNodePorts: List<Int> = replicasPortsByMainNodePort.keys.toList()
        val slotsPerShard: Int = MAX_NUMBER_OF_SLOTS_PER_CLUSTER / shardsMainNodePorts.size
        for (i in shardsMainNodePorts.indices) {
            val port = shardsMainNodePorts.get(i)
            val startSlot = i * slotsPerShard
            val endSlot = if (i == shardsMainNodePorts.size - 1)
                MAX_NUMBER_OF_SLOTS_PER_CLUSTER - 1
            else
                startSlot + slotsPerShard - 1
            try {
                Jedis(CLUSTER_IP, port).use { jedis ->
                    if (port != clusterMeetTarget) {
                        jedis.clusterMeet(CLUSTER_IP, clusterMeetTarget)
                    }
                    val nodeId = jedis.clusterMyId()
                    mainNodeIdsByPort.put(port, nodeId!!)
                    jedis.clusterAddSlots(*IntStream.range(startSlot, endSlot + 1).toArray())
                }
            } catch (e: Exception) {
                throw ValkeyShardedClusterSetupException("Failed creating main node instance at port: $port", e)
            }
        }
    }

    private fun setupReplicas(clusterMeetTarget: Int) {
        for (entry in replicasPortsByMainNodePort.entries) {
            val mainNodeId: String = mainNodeIdsByPort[entry.key] ?: throw ValkeyShardedClusterSetupException(
                "No main node ID found for port: ${entry.key}"
            )
            val replicaPorts: Set<Int> = entry.value
            for (replicaPort in replicaPorts) {
                try {
                    Jedis(CLUSTER_IP, replicaPort).use { jedis ->
                        jedis.clusterMeet(CLUSTER_IP, clusterMeetTarget)
                        waitForNodeToAppearInCluster(jedis, mainNodeId) // make sure main node visible in cluster
                        jedis.clusterReplicate(mainNodeId)
                        waitForClusterToHaveStatusOK(jedis)
                    }
                } catch (e: Exception) {
                    throw ValkeyShardedClusterSetupException(
                        "Failed adding replica instance at port: $replicaPort",
                        e
                    )
                }
            }
        }
    }

    private fun waitForNodeToAppearInCluster(jedis: Jedis, nodeId: String) {
        val nodeReady = waitForPredicateToPass(Supplier { jedis.clusterNodes().contains(nodeId) })
        if (!nodeReady) {
            throw ValkeyShardedClusterSetupException("Node was not ready before timeout")
        }
    }

    private fun waitForClusterToHaveStatusOK(jedis: Jedis) {
        val clusterIsReady = waitForPredicateToPass(Supplier { jedis.clusterInfo().contains("cluster_state:ok") })
        if (!clusterIsReady) {
            throw ValkeyShardedClusterSetupException("Cluster did not have status OK before timeout")
        }
    }

    private fun waitForClusterToBeInteractReady() {
        val clusterIsReady = waitForPredicateToPass(Supplier {
            try {
                JedisCluster(HostAndPort(CLUSTER_IP, nodes.first().port)).use { jc ->
                    jc.get("someKey")
                    return@Supplier true
                }
            } catch (e: Exception) {
                // ignore
                return@Supplier false
            }
        })
        if (!clusterIsReady) {
            throw ValkeyShardedClusterSetupException("Cluster was not stable before timeout")
        }
    }

    private fun waitForPredicateToPass(predicate: Supplier<Boolean>): Boolean {
        val maxWaitInMillis = initializationTimeout.toMillis()

        var waited = 0
        var result: Boolean = predicate.get()
        while (!result && waited < maxWaitInMillis) {
            try {
                Thread.sleep(SLEEP_DURATION_IN_MILLIS)
            } catch (e: InterruptedException) {
                throw ValkeyShardedClusterSetupException("Interrupted while waiting", e)
            }
            waited += SLEEP_DURATION_IN_MILLIS.toInt()
            result = predicate.get()
        }
        return result
    }

    companion object {
        @JvmStatic
        fun builder(): ValkeyShardedClusterBuilder {
            return ValkeyShardedClusterBuilder()
        }
    }
}
