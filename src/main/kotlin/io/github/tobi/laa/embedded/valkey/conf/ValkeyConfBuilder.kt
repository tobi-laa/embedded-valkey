package io.github.tobi.laa.embedded.valkey.conf

import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Path

class ValkeyConfBuilder(private val directives: MutableList<ValkeyDirective> = mutableListOf()) {

    @Throws(IOException::class)
    @JvmOverloads
    fun importConf(conf: Path, charset: Charset = Charsets.UTF_8): ValkeyConfBuilder {
        return importConf(ValkeyConfParser.parse(conf, charset))
    }

    fun importConf(conf: ValkeyConf): ValkeyConfBuilder {
        directives.addAll(conf.directives)
        return this
    }

    fun directive(directive: ValkeyDirective): ValkeyConfBuilder {
        directives.add(directive)
        return this
    }

    fun directive(keyword: String, vararg arguments: String): ValkeyConfBuilder {
        return directive(ValkeyDirective(keyword, *arguments))
    }

    fun bind(address: String): ValkeyConfBuilder {
        return directive(ValkeyDirective.KEYWORD_BIND, address)
    }

    fun binds(vararg addresses: String): ValkeyConfBuilder {
        directives.removeIf { it.keyword == ValkeyDirective.KEYWORD_BIND }
        return directive(ValkeyDirective.KEYWORD_BIND, *addresses)
    }

    fun port(port: Int): ValkeyConfBuilder {
        check(port in 1..65535) { "Port must be between 1 and 65535" }
        directives.removeIf { it.keyword == ValkeyDirective.KEYWORD_PORT }
        return directive(ValkeyDirective.KEYWORD_PORT, port.toString())
    }

    fun replicaOf(host: String, port: Int): ValkeyConfBuilder {
        check(port in 1..65535) { "Port must be between 1 and 65535" }
        directives.removeIf { it.keyword == ValkeyDirective.KEYWORD_REPLICAOF }
        return directive(ValkeyDirective.KEYWORD_REPLICAOF, host, port.toString())
    }

    fun build(): ValkeyConf = ValkeyConf(directives.toList())
}