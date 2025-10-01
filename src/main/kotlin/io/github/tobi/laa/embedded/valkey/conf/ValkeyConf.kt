package io.github.tobi.laa.embedded.valkey.conf

import redis.embedded.Redis

/**
 * Representation of a `valkey.conf` file, i.e. the configuration of a Valkey server.
 */
data class ValkeyConf(val directives: List<ValkeyDirective>) {

    /**
     * Returns the bind addresses configured in this Valkey configuration.
     * @return the bind addresses configured in this Valkey configuration
     */
    fun binds(): List<String> {
        return directives(ValkeyDirective.KEYWORD_BIND).flatMap { it.arguments }
    }

    /**
     * Returns the port configured in this Valkey configuration.
     * If multiple ports are configured, the first one is returned.
     * If no port is configured, `null` is returned.
     * @return the port configured in this Valkey configuration
     */
    fun port(): Int? {
        return directives(ValkeyDirective.KEYWORD_PORT)
            .firstOrNull()
            ?.arguments
            ?.firstOrNull()
            ?.toIntOrNull()
    }

    /**
     * Returns all directives with the given [keyword].
     * @param keyword the keyword of the directives to return
     * @return the directives with the given [keyword]
     */
    fun directives(keyword: String): List<ValkeyDirective> {
        return directives.filter { it.keyword == keyword }
    }

    companion object {

        /**
         * The default Valkey configuration: bind to the loopback address `::1` and use the default Valkey port
         * `6379`.
         */
        @JvmStatic
        val DEFAULT_CONF = ValkeyConfBuilder().bind("::1").port(Redis.DEFAULT_REDIS_PORT).build()
    }
}