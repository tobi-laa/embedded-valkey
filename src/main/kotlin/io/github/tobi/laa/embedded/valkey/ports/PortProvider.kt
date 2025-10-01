package io.github.tobi.laa.embedded.valkey.ports

import java.util.concurrent.ConcurrentSkipListSet

/**
 * This offset is used to calculate the port for the Valkey cluster bus.
 */
internal const val BUS_PORT_OFFSET: Int = 10000

/**
 * Maximum port that can be used for Valkey instances in a cluster setup.
 */
internal const val CLUSTER_MAX_PORT: Int = 65535 - BUS_PORT_OFFSET

/**
 * Is used for providing free ports for Valkey instances.
 */
class PortProvider {

    private val handedOutPorts = ConcurrentSkipListSet<Int>()

    /**
     * Provides the next free port for a Valkey instance.
     * @param sentinel If `true`, the next free port for a Valkey Sentinel instance is provided.
     * Otherwise, the next free port for a Valkey instance is provided.
     * @return The next free port for a Valkey or Valkey Sentinel instance.
     * @throws IllegalStateException If no free port could be found.
     */
    fun next(sentinel: Boolean = false): Int {
        val minPort = if (sentinel) DEFAULT_SENTINEL_PORT else DEFAULT_VALKEY_PORT
        for (candidate in minPort until CLUSTER_MAX_PORT + 1) {
            val candidateBusPort = candidate + BUS_PORT_OFFSET
            if (portCanBeHandedOut(candidate) && portCanBeHandedOut(candidateBusPort)) {
                handedOutPorts.add(candidate)
                handedOutPorts.add(candidateBusPort)
                return candidate
            }
        }
        throw IllegalStateException("Could not find an available TCP port")
    }

    private fun portCanBeHandedOut(port: Int): Boolean {
        return !handedOutPorts.contains(port) && PortChecker.available(port)
    }
}