package io.github.tobi.laa.embedded.valkey.standalone

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import io.github.tobi.laa.embedded.valkey.distribution.ValkeyDistributionProvider
import io.github.tobi.laa.embedded.valkey.process.ValkeyProcess
import redis.embedded.Redis
import java.io.IOException

class ValkeyStandalone(private val distroProvider: ValkeyDistributionProvider, private val conf: ValkeyConf) : Redis {

    private var process: ValkeyProcess? = null

    override fun active() = process?.active ?: false

    @Throws(IOException::class)
    override fun start() {
        val distro = distroProvider.provideDistribution()
        if (process == null) {
            process = ValkeyProcess(valkeyDistribution = distro, config = conf)
        }
        process!!.start()
    }

    @Throws(IOException::class)
    override fun stop() {
        process?.stop()
    }

    override fun ports(): MutableList<Int?> {
        return mutableListOf(conf.port())
    }

    companion object {
        @JvmStatic
        fun builder(): ValkeyStandaloneBuilder {
            return ValkeyStandaloneBuilder()
        }
    }
}
