package io.github.tobi.laa.embedded.valkey.cluster.highavailability

import io.github.tobi.laa.embedded.valkey.ValkeyNode
import io.github.tobi.laa.embedded.valkey.cluster.ValkeyCluster
import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import java.io.IOException

class ValkeyHighAvailability(
    val sentinels: List<ValkeySentinel>,
    val servers: List<ValkeyStandalone>
) : ValkeyCluster {

    override val nodes: List<ValkeyNode> get() = sentinels + servers

    init {
        check(sentinels.isNotEmpty()) { "At least one sentinel must be configured." }
        check(servers.isNotEmpty()) { "At least one server must be configured." }
    }

    @Throws(IOException::class)
    override fun start(awaitReadiness: Boolean, maxWaitTimeSeconds: Long) {
        for (sentinel in sentinels) {
            sentinel.start(awaitReadiness, maxWaitTimeSeconds)
        }
        for (server in servers) {
            server.start(awaitReadiness, maxWaitTimeSeconds)
        }
    }

    @Throws(IOException::class)
    override fun stop(forcibly: Boolean, maxWaitTimeSeconds: Long, removeWorkingDir: Boolean) {
        for (sentinel in sentinels) {
            sentinel.stop(forcibly, maxWaitTimeSeconds, removeWorkingDir)
        }
        for (server in servers) {
            server.stop(forcibly, maxWaitTimeSeconds, removeWorkingDir)
        }
    }

    fun sentinelPorts(): List<Int> {
        return sentinels.map { it.port }.toList()
    }

    fun serverPorts(): List<Int> {
        return servers.map { it.port }.toList()
    }

    companion object {
        @JvmStatic
        fun builder(): ValkeyHighAvailabilityBuilder {
            return ValkeyHighAvailabilityBuilder()
        }
    }
}
