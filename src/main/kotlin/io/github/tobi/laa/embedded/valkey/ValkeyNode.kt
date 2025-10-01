package io.github.tobi.laa.embedded.valkey

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import java.nio.file.Path

/**
 * A [ValkeyNode] represents a single Valkey node instance.
 */
interface ValkeyNode : Valkey {

    /**
     * The port on which the Valkey node is listening.
     */
    val port: Int

    /**
     * The bind addresses of the Valkey node.
     */
    val binds: List<String>

    /**
     * The configuration of the Valkey node.
     */
    val config: ValkeyConf

    /**
     * The working directory of the Valkey node.
     */
    val workingDirectory: Path
}