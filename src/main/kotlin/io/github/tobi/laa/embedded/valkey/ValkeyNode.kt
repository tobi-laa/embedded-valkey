package io.github.tobi.laa.embedded.valkey

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import java.nio.file.Path

/**
 * A [ValkeyNode] represents a single Valkey node instance.
 */
interface ValkeyNode : Valkey {

    /**
     * Indicates whether the Valkey node is currently active (running).
     */
    val active: Boolean

    /**
     * The port on which the Valkey node is listening.
     */
    val port: Int
        get() = config.port()
            ?: throw IllegalStateException("Port not configured")

    /**
     * The bind addresses of the Valkey node.
     */
    val binds: List<String> get() = config.binds()

    /**
     * The configuration of the Valkey node.
     */
    val config: ValkeyConf

    /**
     * The working directory of the Valkey node.
     */
    val workingDirectory: Path
}