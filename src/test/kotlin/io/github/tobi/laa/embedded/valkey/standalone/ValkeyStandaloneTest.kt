package io.github.tobi.laa.embedded.valkey.standalone

import io.github.tobi.laa.embedded.valkey.IntegrationTest
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone.Companion.builder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import redis.clients.jedis.RedisClient
import java.nio.file.Files
import java.nio.file.Path

@IntegrationTest
@DisplayName("Tests for ValkeyStandalone")
internal class ValkeyStandaloneTest {

    @TempDir
    private lateinit var tempDir: Path

    private var valkeyStandalone: ValkeyStandalone? = null
    private var performAction: ThrowableAssert.ThrowingCallable? = null
    private var workingDirectory: Path? = null

    @BeforeEach
    fun reset() {
        valkeyStandalone = null
        performAction = null
        workingDirectory = null
    }

    @Test
    @DisplayName("Starting a standalone Valkey instance with default configuration should work without errors")
    fun `starts with default configuration`() {
        givenStandaloneServer()
        whenServerIsStarted()
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("Calling start multiple times without stopping should not cause errors")
    fun `allows multiple starts without stop`() {
        givenStandaloneServer()
        whenServerIsStartedTwice()
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("Multiple start/stop cycles should work without errors")
    fun `allows multiple start-stop cycles`() {
        givenStandaloneServer()
        whenServerIsStartedAndStoppedThrice()
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("The active flag should be false before the server is started")
    fun `reports inactive before start`() {
        givenStandaloneServer()
        thenServerIsInactive()
    }

    @Test
    @DisplayName("The active flag should be true after the server is started")
    fun `reports active after start`() {
        givenStartedServer()
        thenServerIsActive()
    }

    @Test
    @DisplayName("The active flag should be false after the server is stopped")
    fun `reports inactive after stop`() {
        givenStartedServer()
        whenServerIsStopped()
        thenNoErrorOccurs()
        thenServerIsInactive()
    }

    @Test
    @DisplayName("Accessing workingDirectory should throw IllegalStateException containing 'Process not started' when the server has not been started")
    fun `throws on workingDirectory before start`() {
        givenStandaloneServer()
        whenWorkingDirectoryIsAccessed()
        thenIllegalStateExceptionIsThrownContaining("Process not started")
    }

    @Test
    @DisplayName("The workingDirectory should be an existing directory after the server has started")
    fun `workingDirectory is an existing directory after start`() {
        givenStartedServer()
        whenWorkingDirectoryIsAccessed()
        thenNoErrorOccurs()
        thenWorkingDirectoryIsAnExistingDirectory()
    }

    @Test
    @DisplayName("Stop should be safe to call when the server has not been started")
    fun `stop is safe when not started`() {
        givenStandaloneServer()
        whenServerIsStopped()
        thenNoErrorOccurs()
        thenServerIsInactive()
    }

    @Test
    @DisplayName("It should be possible to read and write data after the server has started")
    fun `supports read and write after start`() {
        givenStartedServer()
        thenDataCanBeReadAndWritten()
    }

    @Test
    @DisplayName("importConf(Path) should apply port configuration from a file to the built server")
    fun `builder importConf from file applies configured port`() {
        givenServerBuiltWithImportedConfFile(port = 6399)
        thenServerHasPort(6399)
    }

    // --- given* ---

    private fun givenStandaloneServer() {
        valkeyStandalone = builder().build()
    }

    private fun givenStartedServer() {
        givenStandaloneServer()
        valkeyStandalone!!.start()
    }

    private fun givenServerBuiltWithImportedConfFile(port: Int) {
        val confFile = tempDir.resolve("test.conf")
        Files.writeString(confFile, "port $port" + System.lineSeparator())
        valkeyStandalone = builder().importConf(confFile).build()
    }

    // --- when* ---

    private fun whenServerIsStarted() {
        performAction = ThrowableAssert.ThrowingCallable { valkeyStandalone!!.start() }
    }

    private fun whenServerIsStartedTwice() {
        performAction = ThrowableAssert.ThrowingCallable {
            valkeyStandalone!!.start()
            valkeyStandalone!!.start()
        }
    }

    private fun whenServerIsStartedAndStoppedThrice() {
        performAction = ThrowableAssert.ThrowingCallable {
            repeat(3) {
                valkeyStandalone!!.start()
                valkeyStandalone!!.stop()
            }
        }
    }

    private fun whenServerIsStopped() {
        performAction = ThrowableAssert.ThrowingCallable { valkeyStandalone!!.stop() }
    }

    private fun whenWorkingDirectoryIsAccessed() {
        performAction = ThrowableAssert.ThrowingCallable { workingDirectory = valkeyStandalone!!.workingDirectory }
    }

    // --- then* ---

    private fun thenNoErrorOccurs() {
        assertThatCode(performAction!!).doesNotThrowAnyException()
    }

    private fun thenIllegalStateExceptionIsThrownContaining(message: String) {
        assertThatCode(performAction!!).isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining(message)
    }

    private fun thenServerIsActive() {
        assertThat(valkeyStandalone!!.active).isTrue()
    }

    private fun thenServerIsInactive() {
        assertThat(valkeyStandalone!!.active).isFalse()
    }

    private fun thenWorkingDirectoryIsAnExistingDirectory() {
        assertThat(workingDirectory).isNotNull()
        assertThat(workingDirectory!!).isDirectory()
    }

    private fun thenServerHasPort(expectedPort: Int) {
        assertThat(valkeyStandalone!!.config.port()).isEqualTo(expectedPort)
    }

    private fun thenDataCanBeReadAndWritten() {
        RedisClient.create("localhost", valkeyStandalone!!.port).use { client ->
            client.mset("abc", "1", "def", "2")
            assertThat(client.mget("abc")[0]).isEqualTo("1")
            assertThat(client.mget("def")[0]).isEqualTo("2")
            assertThat(client.mget("xyz")[0]).isNull()
        }
    }
}
