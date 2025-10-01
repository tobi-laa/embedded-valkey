package io.github.tobi.laa.embedded.valkey

import java.io.IOException

/**
 * Represents "some kind of" Valkey. Could be a single node or a cluster.
 */
interface Valkey : AutoCloseable {

    /**
     * Starts the Valkey node or cluster.
     *
     * @param awaitReadiness If true, the method will block until the node is (or all nodes are) ready to accept requests. Default is true.
     * @param maxWaitTimeSeconds The maximum time to wait for the node(s) to be ready, in seconds. Default is 10 seconds.
     * @throws IOException If an I/O error occurs during startup.
     */
    @Throws(IOException::class)
    fun start(awaitReadiness: Boolean = true, maxWaitTimeSeconds: Long = 10)

    /**
     * Starts the Valkey node or cluster, waiting for readiness for up to 10 seconds.
     *
     * @throws IOException If an I/O error occurs during startup.
     */
    @Throws(IOException::class)
    fun start() = start(true, 10)

    /**
     * Stops the Valkey node or cluster.
     *
     * @param forcibly If true, the node or cluster will be stopped forcibly. Default is false.
     * @param maxWaitTimeSeconds The maximum time to wait for (all) the node(s) to stop, in seconds. Default is 10 seconds.
     * @param removeWorkingDir If true, the working directory (for each node) will be removed after stopping. Default is false.
     * @throws IOException If an I/O error occurs during shutdown.
     */
    @Throws(IOException::class)
    fun stop(forcibly: Boolean = false, maxWaitTimeSeconds: Long = 10, removeWorkingDir: Boolean = false)

    /**
     * Stops the Valkey node or cluster, without forcing and without removing the working directory, waiting up to 10 seconds.
     *
     * @throws IOException If an I/O error occurs during shutdown.
     */
    @Throws(IOException::class)
    fun stop() = stop(forcibly = false, maxWaitTimeSeconds = 10, removeWorkingDir = false)

    @Throws(IOException::class)
    override fun close() {
        stop(forcibly = false, removeWorkingDir = false)
    }
}