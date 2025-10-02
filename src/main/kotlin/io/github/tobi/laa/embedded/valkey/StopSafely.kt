package io.github.tobi.laa.embedded.valkey

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("io.github.tobi.laa.embedded.valkey")

@Suppress("unused")
internal fun stopSafely(
    valkey: Valkey,
    forcibly: Boolean = false,
    maxWaitTimeSeconds: Long = 10,
    removeWorkingDir: Boolean = false
): Exception? {
    try {
        valkey.stop()
        return null
    } catch (e: Exception) {
        log.error("Failed to stop Valkey $valkey", e)
        return e
    }
}