package io.github.tobi.laa.embedded.valkey.standalone

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.distribution.DEFAULT_PROVIDERS
import io.github.tobi.laa.embedded.valkey.distribution.ValkeyDistributionProvider
import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import redis.embedded.Redis
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class ValkeyStandaloneBuilder {
    private var distributionProvider: ValkeyDistributionProvider = DEFAULT_PROVIDERS[detectOperatingSystem()]!!
    private val valkeyConfBuilder = ValkeyConfBuilder()

    init {
        bind("::1")
        bind("127.0.0.1")
        bind("localhost")
        port(Redis.DEFAULT_REDIS_PORT)
    }

    fun distributionProvider(distributionProvider: ValkeyDistributionProvider): ValkeyStandaloneBuilder {
        this.distributionProvider = distributionProvider
        return this
    }

    fun bind(bind: String): ValkeyStandaloneBuilder {
        valkeyConfBuilder.bind(bind)
        return this
    }

    fun port(port: Int): ValkeyStandaloneBuilder {
        valkeyConfBuilder.port(port)
        return this
    }

    fun replicaOf(hostname: String, port: Int): ValkeyStandaloneBuilder {
        valkeyConfBuilder.replicaOf(hostname, port)
        return this
    }

    @Throws(IOException::class)
    fun importConf(valkeyConf: String): ValkeyStandaloneBuilder {
        return importConf(Paths.get(valkeyConf))
    }

    @Throws(IOException::class)
    fun importConf(valkeyConf: Path): ValkeyStandaloneBuilder {
        valkeyConfBuilder.importConf(valkeyConf)
        return this
    }

    fun importConf(valkeyConf: ValkeyConf): ValkeyStandaloneBuilder {
        valkeyConfBuilder.importConf(valkeyConf)
        return this
    }

    fun directive(keyword: String, vararg arguments: String): ValkeyStandaloneBuilder {
        valkeyConfBuilder.directive(keyword, *arguments)
        return this
    }

    @Throws(IOException::class)
    fun build(): ValkeyStandalone {
        return ValkeyStandalone(distributionProvider, valkeyConfBuilder.build())
    }

    fun clone(): ValkeyStandaloneBuilder {
        return ValkeyStandaloneBuilder()
            .distributionProvider(distributionProvider)
            .importConf(valkeyConfBuilder.build())
    }
}
