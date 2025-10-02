package io.github.tobi.laa.embedded.valkey.sentinel

import io.github.tobi.laa.embedded.valkey.ValkeyNode
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSupplier
import io.github.tobi.laa.embedded.valkey.process.ValkeyProcess
import java.io.IOException
import java.nio.file.Path

class ValkeySentinel(private val installationSupplier: ValkeyInstallationSupplier, override val config: ValkeyConf) :
    ValkeyNode {

    override val active: Boolean get() = process?.active ?: false
    override val port: Int get() = config.port() ?: throw IllegalStateException("Port not configured")
    override val binds: List<String> get() = config.binds()
    override val workingDirectory: Path
        get() = process?.workingDirectory ?: throw IllegalStateException("Process not started")

    private var process: ValkeyProcess? = null

    @Throws(IOException::class)
    override fun start(awaitReadiness: Boolean, maxWaitTimeSeconds: Long) {
        val distro = installationSupplier.installValkey()
        if (process == null) {
            process = ValkeyProcess(valkeyInstallation = distro, config = config, sentinel = true)
        }
        process!!.start(awaitReadiness, maxWaitTimeSeconds)
    }

    @Throws(IOException::class)
    override fun stop(forcibly: Boolean, maxWaitTimeSeconds: Long, removeWorkingDir: Boolean) {
        process?.stop(forcibly, maxWaitTimeSeconds, removeWorkingDir)
    }

    companion object {
        @JvmStatic
        fun builder(): ValkeySentinelBuilder {
            return ValkeySentinelBuilder()
        }
    }
}