package io.github.tobi.laa.embedded.valkey.sentinel

import io.github.tobi.laa.embedded.valkey.cluster.highavailability.ReplicationGroup
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.conf.ValkeyDirective
import io.github.tobi.laa.embedded.valkey.distribution.DEFAULT_PROVIDERS
import io.github.tobi.laa.embedded.valkey.distribution.ValkeyDistributionProvider
import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import io.github.tobi.laa.embedded.valkey.ports.DEFAULT_VALKEY_PORT
import io.github.tobi.laa.embedded.valkey.ports.PortProvider
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class ValkeySentinelBuilder
private constructor(private val valkeyConfBuilder: ValkeyConfBuilder = ValkeyConfBuilder()) {

    constructor() : this(ValkeyConfBuilder())

    private var distributionProvider: ValkeyDistributionProvider = DEFAULT_PROVIDERS[detectOperatingSystem()]!!
    private var portProvider: PortProvider = PortProvider()
    private var downAfterMilliseconds = 60000L
    private var failOverTimeout = 180000L
    private var parallelSyncs = 1
    private var quorumSize = 1
    private var replicationGroups: MutableList<ReplicationGroup> = mutableListOf()

    init {
        valkeyConfBuilder.binds("::1", "127.0.0.1")
    }

    fun distributionProvider(distributionProvider: ValkeyDistributionProvider): ValkeySentinelBuilder {
        this.distributionProvider = distributionProvider
        return this
    }

    fun bind(bind: String): ValkeySentinelBuilder {
        valkeyConfBuilder.binds(bind)
        return this
    }

    fun port(port: Int): ValkeySentinelBuilder {
        valkeyConfBuilder.port(port)
        return this
    }

    fun quorumSize(quorumSize: Int): ValkeySentinelBuilder {
        this.quorumSize = quorumSize
        return this
    }

    fun downAfterMilliseconds(downAfterMilliseconds: Long): ValkeySentinelBuilder {
        this.downAfterMilliseconds = downAfterMilliseconds
        return this
    }

    fun failOverTimeout(failoverTimeout: Long): ValkeySentinelBuilder {
        this.failOverTimeout = failoverTimeout
        return this
    }

    fun parallelSyncs(parallelSyncs: Int): ValkeySentinelBuilder {
        this.parallelSyncs = parallelSyncs
        return this
    }

    @Throws(IOException::class)
    fun importConf(valkeyConf: kotlin.String): ValkeySentinelBuilder {
        return importConf(Paths.get(valkeyConf))
    }

    @Throws(IOException::class)
    fun importConf(valkeyConf: Path): ValkeySentinelBuilder {
        valkeyConfBuilder.importConf(valkeyConf)
        return this
    }

    fun importConf(valkeyConf: ValkeyConf): ValkeySentinelBuilder {
        valkeyConfBuilder.importConf(valkeyConf)
        return this
    }

    fun directive(keyword: String, vararg arguments: Any): ValkeySentinelBuilder {
        valkeyConfBuilder.directive(ValkeyDirective(keyword, arguments.map { it.toString() }.toList()))
        return this
    }

    fun build(): ValkeySentinel {
        if (replicationGroups.isEmpty()) {
            monitor("mymain", DEFAULT_VALKEY_PORT)
        } else {
            replicationGroups.forEach {
                monitor(it.mainNodeName, it.mainNodePort)
            }
        }
        if ((valkeyConfBuilder.port() ?: 0) == 0) {
            valkeyConfBuilder.port(portProvider.next(sentinel = true))
        }
        return ValkeySentinel(
            distroProvider = distributionProvider,
            config = valkeyConfBuilder.build(),
        )
    }

    fun monitor(replicationGroup: ReplicationGroup) {
        replicationGroups.add(replicationGroup)

    }

    fun monitor(mainNodeName: String, mainNodePort: Int): ValkeySentinelBuilder {
        directive(
            "sentinel",
            "monitor",
            mainNodeName,
            "127.0.0.1",
            mainNodePort,
            quorumSize
        )
        directive("sentinel", "down-after-milliseconds", mainNodeName, downAfterMilliseconds)
        directive("sentinel", "failover-timeout", mainNodeName, failOverTimeout)
        directive("sentinel", "parallel-syncs", mainNodeName, parallelSyncs)
        return this
    }

    fun clone(): ValkeySentinelBuilder {
        return ValkeySentinelBuilder(valkeyConfBuilder = ValkeyConfBuilder().importConf(valkeyConfBuilder.build()))
            .distributionProvider(distributionProvider)
            .downAfterMilliseconds(downAfterMilliseconds)
            .failOverTimeout(failOverTimeout)
            .parallelSyncs(parallelSyncs)
            .quorumSize(quorumSize)
    }
}
