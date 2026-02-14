package io.github.tobi.laa.embedded.valkey

import io.github.tobi.laa.embedded.valkey.cluster.ValkeyCluster
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Tests for Valkey, ValkeyNode, and ValkeyCluster interfaces")
class ValkeyInterfacesTest {

    private class RecordingValkey : Valkey {
        var startArgs: Pair<Boolean, Long>? = null
        var stopArgs: Triple<Boolean, Long, Boolean>? = null

        override fun start(awaitReadiness: Boolean, maxWaitTimeSeconds: Long) {
            startArgs = awaitReadiness to maxWaitTimeSeconds
        }

        override fun stop(forcibly: Boolean, maxWaitTimeSeconds: Long, removeWorkingDir: Boolean) {
            stopArgs = Triple(forcibly, maxWaitTimeSeconds, removeWorkingDir)
        }
    }

    private class RecordingNode(override val config: io.github.tobi.laa.embedded.valkey.conf.ValkeyConf) : ValkeyNode {
        override val active: Boolean = false
        override val workingDirectory: Path = Files.createTempDirectory("valkey-node")

        override fun start(awaitReadiness: Boolean, maxWaitTimeSeconds: Long) {
            throw UnsupportedOperationException("not needed")
        }

        override fun stop(forcibly: Boolean, maxWaitTimeSeconds: Long, removeWorkingDir: Boolean) {
            throw UnsupportedOperationException("not needed")
        }
    }

    private class RecordingCluster : ValkeyCluster {
        var startArgs: Pair<Boolean, Long>? = null
        var stopArgs: Triple<Boolean, Long, Boolean>? = null
        override val nodes: List<ValkeyNode> = emptyList()

        override fun start(awaitReadiness: Boolean, maxWaitTimeSeconds: Long) {
            startArgs = awaitReadiness to maxWaitTimeSeconds
        }

        override fun stop(forcibly: Boolean, maxWaitTimeSeconds: Long, removeWorkingDir: Boolean) {
            stopArgs = Triple(forcibly, maxWaitTimeSeconds, removeWorkingDir)
        }
    }

    @Test
    @DisplayName("Default start and stop should delegate with default parameter values")
    fun `default start and stop delegate with defaults`() {
        val valkey = RecordingValkey()

        valkey.start()
        valkey.stop()

        assertEquals(true to 10L, valkey.startArgs)
        assertEquals(Triple(false, 10L, false), valkey.stopArgs)
    }

    @Test
    @DisplayName("Partial-arg start should fill in default for maxWaitTimeSeconds")
    fun `start with only awaitReadiness fills default maxWaitTimeSeconds`() {
        val valkey = RecordingValkey()
        valkey.start(awaitReadiness = false)
        assertEquals(false to 10L, valkey.startArgs)
    }

    @Test
    @DisplayName("Partial-arg start should fill in default for awaitReadiness")
    fun `start with only maxWaitTimeSeconds fills default awaitReadiness`() {
        val valkey = RecordingValkey()
        valkey.start(maxWaitTimeSeconds = 5)
        assertEquals(true to 5L, valkey.startArgs)
    }

    @Test
    @DisplayName("Partial-arg stop should fill in defaults for maxWaitTimeSeconds and removeWorkingDir")
    fun `stop with only forcibly fills defaults`() {
        val valkey = RecordingValkey()
        valkey.stop(forcibly = true)
        assertEquals(Triple(true, 10L, false), valkey.stopArgs)
    }

    @Test
    @DisplayName("Partial-arg stop should fill in default for removeWorkingDir")
    fun `stop with forcibly and maxWaitTimeSeconds fills default removeWorkingDir`() {
        val valkey = RecordingValkey()
        valkey.stop(forcibly = true, maxWaitTimeSeconds = 5)
        assertEquals(Triple(true, 5L, false), valkey.stopArgs)
    }

    @Test
    @DisplayName("Partial-arg stop should fill in defaults for forcibly and maxWaitTimeSeconds")
    fun `stop with only removeWorkingDir fills defaults`() {
        val valkey = RecordingValkey()
        valkey.stop(removeWorkingDir = true)
        assertEquals(Triple(false, 10L, true), valkey.stopArgs)
    }

    @Test
    @DisplayName("Cluster default start and stop should delegate with default parameter values")
    fun `cluster default start and stop delegate with defaults`() {
        val cluster = RecordingCluster()
        val asValkey: Valkey = cluster

        asValkey.start()
        asValkey.stop()

        assertEquals(true to 10L, cluster.startArgs)
        assertEquals(Triple(false, 10L, false), cluster.stopArgs)
    }

    @Test
    @DisplayName("Cluster partial-arg start should fill in default maxWaitTimeSeconds")
    fun `cluster start with only awaitReadiness fills default`() {
        val cluster = RecordingCluster()
        cluster.start(awaitReadiness = false)
        assertEquals(false to 10L, cluster.startArgs)
    }

    @Test
    @DisplayName("Cluster partial-arg stop should fill in defaults")
    fun `cluster stop with only forcibly fills defaults`() {
        val cluster = RecordingCluster()
        cluster.stop(forcibly = true)
        assertEquals(Triple(true, 10L, false), cluster.stopArgs)
    }

    @Test
    @DisplayName("Node should expose port and bind addresses from its configuration")
    fun `node exposes port and binds from config`() {
        val conf = ValkeyConfBuilder().port(6380).binds("127.0.0.1", "::1").build()
        val node = RecordingNode(conf)

        assertEquals(6380, node.port)
        assertEquals(listOf("127.0.0.1", "::1"), node.binds)
    }

    @Test
    @DisplayName("Node port should throw IllegalStateException when port is not configured")
    fun `node port throws if not configured`() {
        val conf = ValkeyConfBuilder().build()
        val node = RecordingNode(conf)

        assertThrows(IllegalStateException::class.java) { node.port }
    }
}
