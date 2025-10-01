package io.github.tobi.laa.embedded.valkey

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("io.github.tobi.laa.embedded.valkey")

internal fun stopSafely(valkey: Valkey): Exception? {
    try {
        valkey.stop()
        return null
    } catch (e: Exception) {
        log.error("Failed to stop Valkey $valkey", e)
        return e
    }
}