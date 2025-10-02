package io.github.tobi.laa.embedded.valkey.standalone

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.installation.DEFAULT_PROVIDERS
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSupplier
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import io.github.tobi.laa.embedded.valkey.ports.PortProvider
import java.io.IOException
import java.nio.file.Path

class ValkeyStandaloneBuilder {

    private var customDistroProviders: MutableMap<OperatingSystem, ValkeyInstallationSupplier> = HashMap()
    private var portProvider: PortProvider = PortProvider()
    private val valkeyConfBuilder = ValkeyConfBuilder()

    fun distributionProvider(
        operatingSystem: OperatingSystem,
        distributionProvider: ValkeyInstallationSupplier
    ): ValkeyStandaloneBuilder {
        customDistroProviders[operatingSystem] = distributionProvider
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
        if ((valkeyConfBuilder.port() ?: 0) == 0) {
            valkeyConfBuilder.port(portProvider.next())
        }
        if (valkeyConfBuilder.binds().isEmpty()) {
            valkeyConfBuilder.binds("::1", "127.0.0.1")
        }
        val operatingSystem = detectOperatingSystem()
        val distributionProvider = customDistroProviders[operatingSystem] ?: DEFAULT_PROVIDERS[operatingSystem]
        ?: throw IllegalStateException("No ValkeyDistributionProvider configured for current OS")
        return ValkeyStandalone(distributionProvider, valkeyConfBuilder.build())
    }

    fun clone(): ValkeyStandaloneBuilder {
        val clonedBuilder = ValkeyStandaloneBuilder()
            .importConf(valkeyConfBuilder.build())
        clonedBuilder.customDistroProviders = HashMap(customDistroProviders)
        return clonedBuilder
    }
}
