package io.github.tobi.laa.embedded.valkey.testing

import org.awaitility.Awaitility.await
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool
import java.time.Duration

/**
 * Waits until a sentinel-monitored master becomes available and writable,
 * then returns a [JedisSentinelPool] connected to it.
 */
fun awaitMasterPool(masterName: String, sentinelHosts: Set<String>): JedisSentinelPool {
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(250))
        .until { isMasterWritable(masterName, sentinelHosts) }
    return JedisSentinelPool(masterName, sentinelHosts)
}

private fun isMasterWritable(masterName: String, sentinelHosts: Set<String>): Boolean {
    for (sentinelHost in sentinelHosts) {
        val masterAddr = queryMasterAddr(sentinelHost, masterName) ?: continue
        if (probeMasterWritability(masterAddr)) {
            return true
        }
    }
    return false
}

private fun queryMasterAddr(sentinelHost: String, masterName: String): Pair<String, Int>? {
    val (host, port) = sentinelHost.split(":", limit = 2)
    return try {
        Jedis(host, port.toInt()).use { sentinel ->
            val addr = sentinel.sentinelGetMasterAddrByName(masterName)
            if (addr != null && addr.size >= 2) Pair(addr[0], addr[1].toInt()) else null
        }
    } catch (_: Exception) {
        null
    }
}

private fun probeMasterWritability(masterAddr: Pair<String, Int>): Boolean {
    return try {
        Jedis(masterAddr.first, masterAddr.second).use { master ->
            master.set("__embedded_valkey_master_probe__", "1")
            master.del("__embedded_valkey_master_probe__")
            true
        }
    } catch (_: Exception) {
        false
    }
}
