package io.github.tobi.laa.embedded.valkey.process

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallation
import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import io.github.tobi.laa.embedded.valkey.testing.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.*
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Tests for ValkeyProcess")
class ValkeyProcessTest {

    private var givenProcess: ValkeyProcess? = null
    private var performAction: ThrowableAssert.ThrowingCallable? = null
    private var capturedWorkingDirectory: Path? = null

    @BeforeEach
    fun reset() {
        givenProcess = null
        performAction = null
        capturedWorkingDirectory = null
    }

    @AfterEach
    fun cleanUpProcess() {
        givenProcess?.run { if (active) stop(forcibly = true, maxWaitTimeSeconds = 1) }
    }

    @Test
    @DisplayName("Process should start and stop successfully, and a second start call on an already-running process should be a no-op")
    fun `starts and stops with readiness, second start is idempotent`() {
        givenStartedProcess(ScriptBehavior.ECHO_READY_AND_SLEEP, awaitServerReady = true, maxWaitTimeSeconds = 2)
        whenStartIsCalled(awaitServerReady = true, maxWaitTimeSeconds = 2)
        thenNoErrorOccurs()
        whenStopIsCalled(forcibly = true, maxWaitTimeSeconds = 1)
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("Sentinel process should start and match sentinel-specific readiness output")
    fun `starts sentinel and matches sentinel readiness`() {
        givenProcess = createProcess(ScriptBehavior.ECHO_SENTINEL_READY_AND_SLEEP, sentinel = true)
        whenStartIsCalled(awaitServerReady = true, maxWaitTimeSeconds = 2)
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("Starting should throw IOException when the process terminates prematurely before reporting readiness")
    fun `start throws when process terminates early`() {
        givenProcess = createProcess(ScriptBehavior.EXIT_IMMEDIATELY)
        whenStartIsCalled(awaitServerReady = true, maxWaitTimeSeconds = 1)
        thenIOExceptionIsThrown()
    }

    @Test
    @DisplayName("Stopping a process that was never started should log a warning and not throw")
    fun `stop is safe when never started`() {
        givenProcess = createProcess(ScriptBehavior.EXIT_IMMEDIATELY)
        whenStopIsCalled()
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("Stopping should force termination when the graceful shutdown timeout of 0 seconds is exceeded")
    fun `stop forces termination when graceful shutdown times out`() {
        givenStartedProcess(ScriptBehavior.IGNORE_TERM_AND_SLEEP, awaitServerReady = false, maxWaitTimeSeconds = 1)
        whenStopIsCalled(forcibly = false, maxWaitTimeSeconds = 0)
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("toString should contain 'not yet built' for args and 'not started' for pid before the process is started")
    fun `toString reports not-yet-built state before start`() {
        givenProcess = createProcess(ScriptBehavior.EXIT_IMMEDIATELY)
        thenToStringContains("not yet built", "pid: not started")
    }

    @Test
    @DisplayName("Stopping with removeWorkingDirectory=true should delete the working directory after stop")
    fun `stop removes working directory when requested`() {
        givenStartedProcess(ScriptBehavior.ECHO_READY_AND_SLEEP, awaitServerReady = true, maxWaitTimeSeconds = 2)
        capturedWorkingDirectory = givenProcess!!.workingDirectory
        assertThat(capturedWorkingDirectory).isDirectory()
        whenStopIsCalled(forcibly = true, maxWaitTimeSeconds = 1, removeWorkingDirectory = true)
        thenNoErrorOccurs()
        thenWorkingDirectoryDoesNotExist()
    }

    @Test
    @DisplayName("Starting should throw IOException when the process is alive but never outputs the readiness marker within the timeout")
    fun `start throws when process never becomes ready within timeout`() {
        givenProcess = createProcess(ScriptBehavior.IGNORE_TERM_AND_SLEEP)
        whenStartIsCalled(awaitServerReady = true, maxWaitTimeSeconds = 1)
        thenIOExceptionIsThrown()
    }

    @Test
    @DisplayName("Starting should throw IOException when the process exits unexpectedly during the readiness wait period")
    fun `start throws when process exits during readiness wait`() {
        givenProcess = createProcess(ScriptBehavior.SLEEP_BRIEFLY)
        whenStartIsCalled(awaitServerReady = true, maxWaitTimeSeconds = 3)
        thenIOExceptionIsThrown()
    }

    @Test
    @DisplayName("awaitReady should return without throwing when the process had already output the readiness marker before awaitReady is called with a zero timeout")
    fun `awaitReady succeeds when ready flag is already true`() {
        givenStartedProcess(ScriptBehavior.ECHO_READY_AND_SLEEP, awaitServerReady = false)
        Thread.sleep(1000)
        performAction = ThrowableAssert.ThrowingCallable { givenProcess!!.awaitReady(0) }
        thenNoErrorOccurs()
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    @DisplayName("Constructor should throw IllegalArgumentException when the binary exists but is not executable (Unix only — Windows has no executable permission bits)")
    fun `constructor rejects non-executable binary`() {
        val installDir = Files.createTempDirectory("valkey-install")
        val notExecutable = Files.createTempFile(installDir, "valkey", ".sh")
        notExecutable.toFile().setExecutable(false)
        performAction = ThrowableAssert.ThrowingCallable {
            ValkeyProcess(
                valkeyInstallation = ValkeyInstallation(
                    version = "9.0.3",
                    operatingSystem = detectOperatingSystem(),
                    distributionType = DistributionType.VALKEY,
                    installationPath = installDir,
                    binaryPath = notExecutable
                ),
                config = ValkeyConfBuilder().build()
            )
        }
        thenIllegalArgumentExceptionIsThrown()
    }

    @Test
    @DisplayName("Property accessors should return the values provided at construction time, with defaults applied for unspecified parameters")
    fun `property accessors return configured values`() {
        val installation = createValkeyInstallation(createExecutableScript(ScriptBehavior.EXIT_IMMEDIATELY))
        val config = ValkeyConfBuilder().build()
        givenProcess = ValkeyProcess(valkeyInstallation = installation, config = config)
        assertThat(givenProcess!!.valkeyInstallation).isEqualTo(installation)
        assertThat(givenProcess!!.workingDirectory).exists()
        assertThat(givenProcess!!.config).isEqualTo(config)
        assertThat(givenProcess!!.charset).isEqualTo(Charsets.UTF_8)
        assertThat(givenProcess!!.sentinel).isFalse()
        assertThat(givenProcess!!.active).isFalse()
        assertThat(givenProcess!!.ready).isFalse()
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException when the specified working directory does not exist")
    fun `constructor rejects non-existent working directory`() {
        val installation = createValkeyInstallation(createExecutableScript(ScriptBehavior.EXIT_IMMEDIATELY))
        val nonExistentDir = Path.of(System.getProperty("java.io.tmpdir"), "non-existent-dir-" + System.nanoTime())
        performAction = ThrowableAssert.ThrowingCallable {
            ValkeyProcess(valkeyInstallation = installation, workingDirectory = nonExistentDir, config = ValkeyConfBuilder().build())
        }
        thenIllegalArgumentExceptionIsThrown()
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException when the specified working directory is a regular file rather than a directory")
    fun `constructor rejects working directory that is a regular file`() {
        val installation = createValkeyInstallation(createExecutableScript(ScriptBehavior.EXIT_IMMEDIATELY))
        val regularFile = Files.createTempFile("not-a-dir", ".txt")
        performAction = ThrowableAssert.ThrowingCallable {
            ValkeyProcess(valkeyInstallation = installation, workingDirectory = regularFile, config = ValkeyConfBuilder().build())
        }
        thenIllegalArgumentExceptionIsThrown()
    }

    @Test
    @DisplayName("Stderr output from the running process should be captured by the consuming thread")
    fun `stderr output is captured`() {
        givenStartedProcess(ScriptBehavior.ECHO_READY_AND_STDERR_AND_SLEEP, awaitServerReady = true, maxWaitTimeSeconds = 2)
        Thread.sleep(200)
        whenStopIsCalled(forcibly = true, maxWaitTimeSeconds = 1)
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("toString should include the process PID and built args after the process has started, and not contain placeholder strings")
    fun `toString includes pid and args after start`() {
        givenStartedProcess(ScriptBehavior.ECHO_READY_AND_SLEEP, awaitServerReady = true, maxWaitTimeSeconds = 2)
        thenToStringContains("pid:")
        thenToStringDoesNotContain("not started", "not yet built")
    }

    private fun createProcess(behavior: ScriptBehavior, sentinel: Boolean = false): ValkeyProcess {
        val installation = createValkeyInstallation(createExecutableScript(behavior))
        return ValkeyProcess(valkeyInstallation = installation, config = ValkeyConfBuilder().build(), sentinel = sentinel)
    }

    private fun givenStartedProcess(behavior: ScriptBehavior, awaitServerReady: Boolean = true, maxWaitTimeSeconds: Long = 10) {
        givenProcess = createProcess(behavior)
        givenProcess!!.start(awaitServerReady = awaitServerReady, maxWaitTimeSeconds = maxWaitTimeSeconds)
    }

    private fun whenStartIsCalled(awaitServerReady: Boolean = true, maxWaitTimeSeconds: Long = 10) {
        performAction = ThrowableAssert.ThrowingCallable {
            givenProcess!!.start(awaitServerReady = awaitServerReady, maxWaitTimeSeconds = maxWaitTimeSeconds)
        }
    }

    private fun whenStopIsCalled(forcibly: Boolean = false, maxWaitTimeSeconds: Long = 10, removeWorkingDirectory: Boolean = false) {
        performAction = ThrowableAssert.ThrowingCallable {
            givenProcess!!.stop(forcibly = forcibly, maxWaitTimeSeconds = maxWaitTimeSeconds, removeWorkingDirectory = removeWorkingDirectory)
        }
    }

    private fun thenNoErrorOccurs() {
        assertThatCode(performAction!!).doesNotThrowAnyException()
    }

    private fun thenIOExceptionIsThrown() {
        assertThatCode(performAction!!).isExactlyInstanceOf(java.io.IOException::class.java)
    }

    private fun thenIllegalArgumentExceptionIsThrown() {
        assertThatCode(performAction!!).isExactlyInstanceOf(IllegalArgumentException::class.java)
    }

    private fun thenToStringContains(vararg parts: String) {
        assertThat(givenProcess!!.toString()).contains(*parts)
    }

    private fun thenToStringDoesNotContain(vararg parts: String) {
        assertThat(givenProcess!!.toString()).doesNotContain(*parts)
    }

    private fun thenWorkingDirectoryDoesNotExist() {
        assertThat(capturedWorkingDirectory).doesNotExist()
    }
}
