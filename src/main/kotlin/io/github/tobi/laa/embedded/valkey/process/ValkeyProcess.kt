package io.github.tobi.laa.embedded.valkey.process

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfWriter
import io.github.tobi.laa.embedded.valkey.distribution.ValkeyDistribution
import org.slf4j.LoggerFactory.getLogger
import org.slf4j.event.Level
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.locks.ReentrantLock
import kotlin.io.path.absolutePathString

private val SERVER_READY_PATTERN = Regex(".*[Rr]eady to accept connections.*")

class ValkeyProcess
@JvmOverloads
constructor(
    val valkeyDistribution: ValkeyDistribution,
    val workingDirectory: Path = Files.createTempDirectory(
        valkeyDistribution.installationPath,
        "${valkeyDistribution.distributionType.name.lowercase()}-${valkeyDistribution.version}-${valkeyDistribution.operatingSystem.name.lowercase()}"
    ),
    val config: ValkeyConf = ValkeyConf.DEFAULT_CONF,
    val charset: Charset = Charsets.UTF_8,
    val args: List<String> = emptyList(),
    internal val stdoutLogLevel: Level = Level.DEBUG,
    internal val stderrLogLevel: Level = Level.ERROR
) {

    private val log = getLogger(ValkeyProcess::class.java)

    private val lock = ReentrantLock()

    private val configFile: Path = workingDirectory.resolve("valkey.conf")

    private lateinit var process: Process

    private lateinit var stdoutConsumingThread: Thread

    private lateinit var stderrConsumingThread: Thread

    var active: Boolean = false
        private set

    var ready: Boolean = false
        private set

    init {
        require(Files.exists(valkeyDistribution.binaryPath)) {
            "${valkeyDistribution.distributionType.displayName} binary does not exist at path: ${valkeyDistribution.binaryPath}"
        }
        require(Files.isExecutable(valkeyDistribution.binaryPath)) {
            "${valkeyDistribution.distributionType.displayName} binary is not executable at path: ${valkeyDistribution.binaryPath}"
        }
        require(Files.exists(workingDirectory)) {
            "Working directory does not exist at path: $workingDirectory"
        }
        require(Files.isDirectory(workingDirectory)) {
            "Working directory is not a directory at path: $workingDirectory"
        }
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun start(awaitServerReady: Boolean = true, maxWaitTimeSeconds: Long = 10) {
        lock.lock()
        try {
            if (active) {
                log.warn("Process has already been started: {}", this)
                return
            }
            log.info("🚀 Starting Valkey process: {}", this)
            createValkeyConfFile()
            buildAndStartProcess()
            startStdoutStderrConsumingThreads()
            addShutdownHook()
            if (awaitServerReady) {
                awaitReady(maxWaitTimeSeconds)
            }
            active = true
        } finally {
            lock.unlock()
        }
    }

    private fun createValkeyConfFile() {
        ValkeyConfWriter.write(config, configFile, charset)
        log.debug("✍️ Wrote Valkey config to file: {}", configFile)
    }

    private fun buildAndStartProcess() {
        val processBuilder = ProcessBuilder()
            .command(listOf(valkeyDistribution.binaryPath.toString()) + configFile.absolutePathString() + args)
            .directory(workingDirectory.toFile())
        process = processBuilder.start()
    }

    private fun startStdoutStderrConsumingThreads() {
        stdoutConsumingThread = Thread(
            {
                try {
                    process.inputReader().useLines { lines ->
                        lines.forEach { line ->
                            if (SERVER_READY_PATTERN.matches(line)) {
                                ready = true
                            }
                            log.atLevel(stdoutLogLevel)
                                .log(
                                    "[{} v{} - pid: {} - stdout] {}",
                                    valkeyDistribution.distributionType.displayName,
                                    valkeyDistribution.version,
                                    process.pid(),
                                    line
                                )
                        }
                    }
                } catch (e: Exception) {
                    log.trace("Error while consuming stdout for {}, probably the process has been stopped.", this, e)
                }
            },
            "${valkeyDistribution.distributionType.name.lowercase()}-${process.pid()}-stdout-consuming-thread"
        )
        stdoutConsumingThread.start()

        stderrConsumingThread = Thread(
            {
                try {
                    process.errorReader().useLines { lines ->
                        lines.forEach { line ->
                            log.atLevel(stderrLogLevel).log(
                                "[{} v{} - pid: {} - stderr] {}",
                                valkeyDistribution.distributionType.displayName,
                                valkeyDistribution.version,
                                process.pid(),
                                line
                            )
                        }
                    }
                } catch (e: Exception) {
                    log.trace("Error while consuming stderr for {}, probably the process has been stopped.", this, e)
                }
            },
            "${valkeyDistribution.distributionType.name.lowercase()}-${process.pid()}-stderr-consuming-thread"
        )
        stderrConsumingThread.start()
    }

    private fun addShutdownHook() {
        Runtime.getRuntime()
            .addShutdownHook(
                Thread(
                    { stop() },
                    "${valkeyDistribution.distributionType.name.lowercase()}-${process.pid()}-shutdown-hook"
                )
            )
    }

    private fun awaitReady(maxWaitTimeSeconds: Long = 10) {
        for (i in 0 until maxWaitTimeSeconds) {
            if (ready) {
                return
            }
            if (!process.isAlive) {
                throw IOException("Process terminated unexpectedly: $this")
            }
            SECONDS.sleep(1)
        }
        if (!ready) {
            throw IOException("Process did not become ready within $maxWaitTimeSeconds seconds: $this")
        }
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun stop(forcibly: Boolean = false, maxWaitTimeSeconds: Long = 10, removeWorkingDirectory: Boolean = false) {
        lock.lock()
        try {
            if (!active) {
                log.warn("Process has already been stopped: {}", this)
                return
            }
            log.info("🫷 Stopping Valkey process: {}", this)
            stopProcess(forcibly, maxWaitTimeSeconds)
            stopStdoutStderrConsumingThreads()
            if (removeWorkingDirectory) {
                deleteWorkingDirectory()
            }
            ready = false
            active = false
        } finally {
            lock.unlock()
        }
    }

    private fun stopProcess(forcibly: Boolean = false, maxWaitTimeSeconds: Long = 10) {
        if (forcibly) {
            process.destroyForcibly().waitFor()
        } else {
            process.destroy()
            if (!process.waitFor(maxWaitTimeSeconds, SECONDS)) {
                log.warn("Process did not terminate within 10 seconds, forcing termination: {}", this)
                process.destroyForcibly().waitFor()
            }
        }
    }

    private fun stopStdoutStderrConsumingThreads() {
        try {
            if (this::stdoutConsumingThread.isInitialized && stdoutConsumingThread.isAlive) {
                stdoutConsumingThread.interrupt()
                stdoutConsumingThread.join()
            }
        } finally {
            if (this::stderrConsumingThread.isInitialized && stderrConsumingThread.isAlive) {
                stderrConsumingThread.interrupt()
                stderrConsumingThread.join()
            }
        }
    }

    private fun deleteWorkingDirectory() {
        workingDirectory.toFile().deleteRecursively()
    }

    override fun toString(): String {
        return "${valkeyDistribution.distributionType.displayName} v${valkeyDistribution.version} for ${valkeyDistribution.operatingSystem.displayName} (working directory: $workingDirectory, args: $args, pid: ${if (this::process.isInitialized) process.pid() else "not started"})"
    }
}