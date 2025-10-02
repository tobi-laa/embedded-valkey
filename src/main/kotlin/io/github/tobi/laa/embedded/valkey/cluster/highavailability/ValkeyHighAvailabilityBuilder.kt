package io.github.tobi.laa.embedded.valkey.cluster.highavailability

import io.github.tobi.laa.embedded.valkey.ports.PortProvider
import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel
import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinelBuilder
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandaloneBuilder
import java.io.IOException
import java.util.*

class ValkeyHighAvailabilityBuilder {

    private var sentinelBuilder = ValkeySentinelBuilder()
    private var serverBuilder = ValkeyStandaloneBuilder()
    private var sentinelCount = 1
    private var portProvider: PortProvider = PortProvider()
    private val groups: MutableList<ReplicationGroup> = LinkedList<ReplicationGroup>()

    fun withSentinelBuilder(sentinelBuilder: ValkeySentinelBuilder): ValkeyHighAvailabilityBuilder {
        this.sentinelBuilder = sentinelBuilder
        return this
    }

    fun withServerBuilder(serverBuilder: ValkeyStandaloneBuilder): ValkeyHighAvailabilityBuilder {
        this.serverBuilder = serverBuilder
        return this
    }

    fun sentinelCount(sentinelCount: Int): ValkeyHighAvailabilityBuilder {
        this.sentinelCount = sentinelCount
        return this
    }

    fun quorumSize(quorumSize: Int): ValkeyHighAvailabilityBuilder {
        this.sentinelBuilder.quorumSize(quorumSize)
        return this
    }

    fun replicationGroup(masterName: String, replicaCount: Int): ValkeyHighAvailabilityBuilder {
        this.groups.add(ReplicationGroup(masterName, portProvider, replicaCount))
        return this
    }

    @Throws(IOException::class)
    fun build(): ValkeyHighAvailability {
        val sentinels = buildSentinels()
        val servers = buildServers()
        return ValkeyHighAvailability(sentinels, servers)
    }

    @Throws(IOException::class)
    private fun buildServers(): List<ValkeyStandalone> {
        val servers: MutableList<ValkeyStandalone> = ArrayList<ValkeyStandalone>()
        for (g in groups) {
            servers.add(buildMaster(g))
            buildSlaves(servers, g)
        }
        return servers.toList()
    }

    @Throws(IOException::class)
    private fun buildSlaves(servers: MutableList<ValkeyStandalone>, g: ReplicationGroup) {
        for (slavePort in g.replicaPorts) {
            val replicaBuilder = serverBuilder.clone()
            replicaBuilder.port(slavePort)
            replicaBuilder.replicaOf("localhost", g.mainNodePort)
            val slave = replicaBuilder.build()
            servers.add(slave)
        }
    }

    @Throws(IOException::class)
    private fun buildMaster(g: ReplicationGroup): ValkeyStandalone {
        return serverBuilder.clone().port(g.mainNodePort).build()
    }

    private fun buildSentinels(): List<ValkeySentinel> {
        var toBuild = sentinelCount
        val sentinels: MutableList<ValkeySentinel> = ArrayList<ValkeySentinel>()
        while (toBuild-- > 0) {
            sentinels.add(buildSentinel())
        }
        return sentinels.toList()
    }

    private fun buildSentinel(): ValkeySentinel {
        val sentinelBuilder = this.sentinelBuilder.clone()
        sentinelBuilder.port(nextSentinelPort())
        for (group in groups) {
            sentinelBuilder.monitor(group)
        }
        return sentinelBuilder.build()
    }

    private fun nextSentinelPort(): Int {
        return portProvider.next(sentinel = true)
    }
}
