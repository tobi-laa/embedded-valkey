package io.github.tobi.laa.embedded.valkey

import io.github.tobi.laa.embedded.valkey.cluster.ValkeyCluster
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests for Valkey, ValkeyNode, and ValkeyCluster interfaces")
class ValkeyInterfacesTest {

    @Test
    @DisplayName("Default no-arg start() should delegate with awaitReadiness=true and maxWaitTimeSeconds=10")
    fun `default start delegates with defaults`() {
        val valkey = mockk<Valkey>()
        every { valkey.start(any(), any()) } just Runs
        every { valkey.start() } answers { callOriginal() }

        valkey.start()

        verify { valkey.start(awaitReadiness = true, maxWaitTimeSeconds = 10) }
    }

    @Test
    @DisplayName("Default no-arg stop() should delegate with forcibly=false, maxWaitTimeSeconds=10, removeWorkingDir=false")
    fun `default stop delegates with defaults`() {
        val valkey = mockk<Valkey>()
        every { valkey.stop(any(), any(), any()) } just Runs
        every { valkey.stop() } answers { callOriginal() }

        valkey.stop()

        verify { valkey.stop(forcibly = false, maxWaitTimeSeconds = 10, removeWorkingDir = false) }
    }

    @Test
    @DisplayName("start(awaitReadiness=false) should fill in default maxWaitTimeSeconds=10")
    fun `start with only awaitReadiness fills default maxWaitTimeSeconds`() {
        val valkey = mockk<Valkey>()
        every { valkey.start(any(), any()) } just Runs

        valkey.start(awaitReadiness = false)

        verify { valkey.start(awaitReadiness = false, maxWaitTimeSeconds = 10) }
    }

    @Test
    @DisplayName("start(maxWaitTimeSeconds=5) should fill in default awaitReadiness=true")
    fun `start with only maxWaitTimeSeconds fills default awaitReadiness`() {
        val valkey = mockk<Valkey>()
        every { valkey.start(any(), any()) } just Runs

        valkey.start(maxWaitTimeSeconds = 5)

        verify { valkey.start(awaitReadiness = true, maxWaitTimeSeconds = 5) }
    }

    @Test
    @DisplayName("stop(forcibly=true) should fill in defaults maxWaitTimeSeconds=10 and removeWorkingDir=false")
    fun `stop with only forcibly fills defaults`() {
        val valkey = mockk<Valkey>()
        every { valkey.stop(any(), any(), any()) } just Runs

        valkey.stop(forcibly = true)

        verify { valkey.stop(forcibly = true, maxWaitTimeSeconds = 10, removeWorkingDir = false) }
    }

    @Test
    @DisplayName("stop(forcibly=true, maxWaitTimeSeconds=5) should fill in default removeWorkingDir=false")
    fun `stop with forcibly and maxWaitTimeSeconds fills default removeWorkingDir`() {
        val valkey = mockk<Valkey>()
        every { valkey.stop(any(), any(), any()) } just Runs

        valkey.stop(forcibly = true, maxWaitTimeSeconds = 5)

        verify { valkey.stop(forcibly = true, maxWaitTimeSeconds = 5, removeWorkingDir = false) }
    }

    @Test
    @DisplayName("stop(removeWorkingDir=true) should fill in defaults forcibly=false and maxWaitTimeSeconds=10")
    fun `stop with only removeWorkingDir fills defaults`() {
        val valkey = mockk<Valkey>()
        every { valkey.stop(any(), any(), any()) } just Runs

        valkey.stop(removeWorkingDir = true)

        verify { valkey.stop(forcibly = false, maxWaitTimeSeconds = 10, removeWorkingDir = true) }
    }

    @Test
    @DisplayName("Cluster default no-arg start() should delegate with awaitReadiness=true and maxWaitTimeSeconds=10")
    fun `cluster default start delegates with defaults`() {
        val cluster = mockk<ValkeyCluster>()
        every { cluster.start(any(), any()) } just Runs
        every { cluster.start() } answers { callOriginal() }

        cluster.start()

        verify { cluster.start(awaitReadiness = true, maxWaitTimeSeconds = 10) }
    }

    @Test
    @DisplayName("Cluster default no-arg stop() should delegate with forcibly=false, maxWaitTimeSeconds=10, removeWorkingDir=false")
    fun `cluster default stop delegates with defaults`() {
        val cluster = mockk<ValkeyCluster>()
        every { cluster.stop(any(), any(), any()) } just Runs
        every { cluster.stop() } answers { callOriginal() }

        cluster.stop()

        verify { cluster.stop(forcibly = false, maxWaitTimeSeconds = 10, removeWorkingDir = false) }
    }

    @Test
    @DisplayName("Cluster start(awaitReadiness=false) should fill in default maxWaitTimeSeconds=10")
    fun `cluster start with only awaitReadiness fills default`() {
        val cluster = mockk<ValkeyCluster>()
        every { cluster.start(any(), any()) } just Runs

        cluster.start(awaitReadiness = false)

        verify { cluster.start(awaitReadiness = false, maxWaitTimeSeconds = 10) }
    }

    @Test
    @DisplayName("Cluster stop(forcibly=true) should fill in defaults maxWaitTimeSeconds=10 and removeWorkingDir=false")
    fun `cluster stop with only forcibly fills defaults`() {
        val cluster = mockk<ValkeyCluster>()
        every { cluster.stop(any(), any(), any()) } just Runs

        cluster.stop(forcibly = true)

        verify { cluster.stop(forcibly = true, maxWaitTimeSeconds = 10, removeWorkingDir = false) }
    }

    @Test
    @DisplayName("ValkeyNode should expose port and bind addresses from its configuration")
    fun `node exposes port and binds from config`() {
        val conf = ValkeyConfBuilder().port(6380).binds("127.0.0.1", "::1").build()
        val node = mockk<ValkeyNode>()
        every { node.config } returns conf
        every { node.port } answers { callOriginal() }
        every { node.binds } answers { callOriginal() }

        assertThat(node.port).isEqualTo(6380)
        assertThat(node.binds).isEqualTo(listOf("127.0.0.1", "::1"))
    }

    @Test
    @DisplayName("ValkeyNode port should throw IllegalStateException when port is not configured")
    fun `node port throws if not configured`() {
        val conf = ValkeyConfBuilder().build()
        val node = mockk<ValkeyNode>()
        every { node.config } returns conf
        every { node.port } answers { callOriginal() }

        assertThatThrownBy { node.port }.isInstanceOf(IllegalStateException::class.java)
    }
}
