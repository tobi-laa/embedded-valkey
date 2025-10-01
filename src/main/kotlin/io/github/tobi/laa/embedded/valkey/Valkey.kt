package io.github.tobi.laa.embedded.valkey

import java.io.IOException

/**
 * Represents "some kind of" Valkey. Could be a single node or a cluster.
 */
interface Valkey {

    /**
     * Indicates whether the Valkey node or cluster is currently active (running).
     */
    val active: Boolean

    /**
     * Starts the Valkey node or cluster.
     *
     * @param awaitServerReady If true, the method will block until the node is (or all nodes are) ready to accept connections.
     * @param maxWaitTimeSeconds The maximum time to wait for the node(s) to be ready, in seconds.
     * @throws IOException If an I/O error occurs during startup.
     */
    @Throws(IOException::class)
    fun start(awaitServerReady: Boolean = true, maxWaitTimeSeconds: Long = 10)

    /**
     * Stops the Valkey node or cluster.
     *
     * @param forcibly If true, the node or cluster will be stopped forcibly.
     * @param maxWaitTimeSeconds The maximum time to wait for (all) the node(s) to stop, in seconds.
     * @param removeWorkingDir If true, the working directory (for each node) will be removed after stopping.
     * @throws IOException If an I/O error occurs during shutdown.
     */
    @Throws(IOException::class)
    fun stop(forcibly: Boolean = false, maxWaitTimeSeconds: Long = 10, removeWorkingDir: Boolean = false)
}