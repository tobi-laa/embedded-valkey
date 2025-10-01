package io.github.tobi.laa.embedded.valkey.conf

data class ValkeyDirective(val keyword: String, val arguments: List<String>) {

    constructor(keyword: String, vararg arguments: String) : this(keyword, arguments.asList())

    init {
        require(keyword.isNotBlank()) { "Keyword must not be blank" }
        require(keyword.matches(Regex("[a-zA-Z0-9_-]+"))) {
            "Keyword '${keyword}' contains illegal characters. Only alphanumeric characters, hyphens and underscores are allowed"
        }
        require(arguments.isNotEmpty()) { "At least one argument is required" }
    }

    companion object {

        @JvmStatic
        val KEYWORD_PORT = "port"

        @JvmField
        val KEYWORD_BIND = "bind"

        @JvmField
        val KEYWORD_DIR = "dir"

        @JvmField
        val KEYWORD_REPLICAOF = "replicaof"
    }
}