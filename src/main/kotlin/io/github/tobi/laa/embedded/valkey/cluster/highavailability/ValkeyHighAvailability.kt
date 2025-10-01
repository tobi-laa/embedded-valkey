package io.github.tobi.laa.embedded.valkey.cluster.highavailability

import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import redis.embedded.Redis
import java.io.IOException
import java.util.*

class ValkeyHighAvailability(
    val sentinels: List<ValkeySentinel>,
    val servers: List<ValkeyStandalone>
) : Redis {

    init {
        check(sentinels.isNotEmpty()) { "At least one sentinel must be configured." }
        check(servers.isNotEmpty()) { "At least one server must be configured." }
    }

    override fun active(): Boolean {
        for (redis in sentinels) {
            if (!redis.active()) {
                return false
            }
        }
        for (redis in servers) {
            if (!redis.active()) {
                return false
            }
        }
        return true
    }

    @Throws(IOException::class)
    override fun start() {
        for (redis in sentinels) {
            redis.start()
        }
        for (redis in servers) {
            redis.start()
        }
    }

    @Throws(IOException::class)
    override fun stop() {
        for (redis in sentinels) {
            redis.stop()
        }
        for (redis in servers) {
            redis.stop()
        }
    }

    override fun ports(): MutableList<Int?> {
        val ports: MutableList<Int?> = ArrayList<Int?>()
        ports.addAll(sentinelPorts())
        ports.addAll(serverPorts())
        return ports
    }

    fun sentinelPorts(): MutableList<Int?> {
        val ports: MutableList<Int?> = ArrayList<Int?>()
        for (redis in sentinels) {
            ports.addAll(redis.ports())
        }
        return ports
    }

    fun servers(): MutableList<Redis?> {
        return LinkedList<Redis?>(servers)
    }

    fun serverPorts(): MutableList<Int?> {
        val ports: MutableList<Int?> = ArrayList<Int?>()
        for (redis in servers) {
            ports.addAll(redis.ports())
        }
        return ports
    }

    companion object {
        @JvmStatic
        fun builder(): ValkeyHighAvailabilityBuilder {
            return ValkeyHighAvailabilityBuilder()
        }
    }
}
