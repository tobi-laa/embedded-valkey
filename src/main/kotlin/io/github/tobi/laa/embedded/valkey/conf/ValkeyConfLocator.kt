package io.github.tobi.laa.embedded.valkey.conf

import redis.embedded.Redis
import redis.embedded.RedisInstance
import java.nio.file.Path
import java.nio.file.Paths

private val ARGS_PROP = RedisInstance::class.java.declaredFields
    .filter { it.name == "args" }
    .map { field -> field.isAccessible = true; field }
    .first()

/**
 * Locates the temporary Valkey configuration file created by an embedded Valkey server.
 */
object ValkeyConfLocator {

    @Suppress("UNCHECKED_CAST")
    fun locate(server: Redis): Path {
        val args = ARGS_PROP[server as RedisInstance] as List<String>
        val valkeyConf = args.find { it.endsWith(".conf") }?.let { Paths.get(it) }
        return valkeyConf ?: throw IllegalStateException("No config file found for embedded Valkey server: $server")
    }
}