package io.github.tobi.laa.embedded.valkey.cluster.highavailability

import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel
import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinelBuilder
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandaloneBuilder
import redis.embedded.core.PortProvider
import java.io.IOException
import java.util.*

class ValkeyHighAvailabilityBuilder {

    private var sentinelBuilder = ValkeySentinelBuilder()
    private var serverBuilder = ValkeyStandaloneBuilder()
    private var sentinelCount = 1
    private var sentinelPortProvider: PortProvider = PortProvider.newSequencePortProvider(26379)
    private var replicationGroupPortProvider: PortProvider = PortProvider.newSequencePortProvider(6379)
    private val groups: MutableList<ReplicationGroup> = LinkedList<ReplicationGroup>()

    fun withSentinelBuilder(sentinelBuilder: ValkeySentinelBuilder): ValkeyHighAvailabilityBuilder {
        this.sentinelBuilder = sentinelBuilder
        return this
    }

    fun withServerBuilder(serverBuilder: ValkeyStandaloneBuilder): ValkeyHighAvailabilityBuilder {
        this.serverBuilder = serverBuilder
        return this
    }

    fun sentinelPorts(ports: Collection<Int>): ValkeyHighAvailabilityBuilder {
        this.sentinelPortProvider = PortProvider.newPredefinedPortProvider(ports)
        this.sentinelCount = ports.size
        return this
    }

    fun serverPorts(ports: Collection<Int>): ValkeyHighAvailabilityBuilder {
        this.replicationGroupPortProvider = PortProvider.newPredefinedPortProvider(ports)
        return this
    }

    fun ephemeralSentinels(): ValkeyHighAvailabilityBuilder {
        this.sentinelPortProvider = PortProvider.newEphemeralPortProvider()
        return this
    }

    fun ephemeralServers(): ValkeyHighAvailabilityBuilder {
        this.replicationGroupPortProvider = PortProvider.newEphemeralPortProvider()
        return this
    }

    fun ephemeral(): ValkeyHighAvailabilityBuilder {
        ephemeralSentinels()
        ephemeralServers()
        return this
    }

    fun sentinelCount(sentinelCount: Int): ValkeyHighAvailabilityBuilder {
        this.sentinelCount = sentinelCount
        return this
    }

    fun sentinelStartingPort(startingPort: Int): ValkeyHighAvailabilityBuilder {
        this.sentinelPortProvider = PortProvider.newSequencePortProvider(startingPort)
        return this
    }

    fun quorumSize(quorumSize: Int): ValkeyHighAvailabilityBuilder {
        this.sentinelBuilder.quorumSize(quorumSize)
        return this
    }

    fun replicationGroup(masterName: String, slaveCount: Int): ValkeyHighAvailabilityBuilder {
        this.groups.add(ReplicationGroup(masterName, replicationGroupPortProvider, slaveCount))
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
        return sentinelPortProvider.get()
    }
}
