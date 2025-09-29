package io.github.tobi.laa.embedded.valkey.conf

private const val KEYWORD_PORT = "port"
private const val KEYWORD_BIND = "bind"

/**
 * Representation of a `valkey.conf` file, i.e. the configuration of a Valkey server.
 */
data class ValkeyConf(val directives: List<Directive>) {

    /**
     * Returns the bind addresses configured in this Valkey configuration.
     * @return the bind addresses configured in this Valkey configuration
     */
    fun binds(): List<String> {
        return directives(KEYWORD_BIND).map { it.arguments.first() }
    }

    /**
     * Returns all directives with the given [keyword].
     * @param keyword the keyword of the directives to return
     * @return the directives with the given [keyword]
     */
    fun directives(keyword: String): List<Directive> {
        return directives.filter { it.keyword == keyword }
    }

}