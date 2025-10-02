package io.github.tobi.laa.embedded.valkey.standalone

import io.github.tobi.laa.embedded.valkey.ValkeyNode
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSupplier
import io.github.tobi.laa.embedded.valkey.process.ValkeyProcess
import java.io.IOException
import java.nio.file.Path

class ValkeyStandalone internal constructor(
    private val distroProvider: ValkeyInstallationSupplier,
    override val config: ValkeyConf
) :
    ValkeyNode {

    override val active: Boolean get() = process?.active ?: false
    override val port: Int get() = config.port() ?: throw IllegalStateException("Port not configured")
    override val binds: List<String> get() = config.binds()
    override val workingDirectory: Path
        get() = process?.workingDirectory ?: throw IllegalStateException("Process not started")

    private var process: ValkeyProcess? = null

    @Throws(IOException::class)
    override fun start(awaitReadiness: Boolean, maxWaitTimeSeconds: Long) {
        val distro = distroProvider.installValkey()
        if (process == null) {
            process = ValkeyProcess(valkeyInstallation = distro, config = config)
        }
        process!!.start(awaitReadiness, maxWaitTimeSeconds)
    }

    @Throws(IOException::class)
    override fun stop(forcibly: Boolean, maxWaitTimeSeconds: Long, removeWorkingDir: Boolean) {
        process?.stop(forcibly, maxWaitTimeSeconds, removeWorkingDir)
    }

    companion object {
        @JvmStatic
        fun builder(): ValkeyStandaloneBuilder {
            return ValkeyStandaloneBuilder()
        }
    }
}
