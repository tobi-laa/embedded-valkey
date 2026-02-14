package io.github.tobi.laa.embedded.valkey

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

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

    @Test
    fun `default start and stop delegate with defaults`() {
        val valkey = RecordingValkey()

        valkey.start()
        valkey.stop()

        assertEquals(true to 10L, valkey.startArgs)
        assertEquals(Triple(false, 10L, false), valkey.stopArgs)
    }

    @Test
    fun `node exposes port and binds from config`() {
        val conf = ValkeyConfBuilder().port(6380).binds("127.0.0.1", "::1").build()
        val node = RecordingNode(conf)

        assertEquals(6380, node.port)
        assertEquals(listOf("127.0.0.1", "::1"), node.binds)
    }

    @Test
    fun `node port throws if not configured`() {
        val conf = ValkeyConfBuilder().build()
        val node = RecordingNode(conf)

        assertThrows(IllegalStateException::class.java) { node.port }
    }
}
