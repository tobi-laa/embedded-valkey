package io.github.tobi.laa.embedded.valkey.cluster.highavailability

import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool
import java.io.IOException
import java.net.UnknownHostException
import java.util.Set

class ValkeyHighAvailibilityTest {
    private val sentinelBuilder = ValkeySentinel.builder()
    private val bindAddress: String? = null

    private var sentinel1: ValkeySentinel? = null
    private var sentinel2: ValkeySentinel? = null
    private var master1: ValkeyStandalone? = null
    private var master2: ValkeyStandalone? = null

    private val instance: ValkeyHighAvailability? = null

    @Before
    @Throws(UnknownHostException::class)
    fun setUp() {
        sentinel1 = Mockito.mock<ValkeySentinel?>(ValkeySentinel::class.java)
        sentinel2 = Mockito.mock<ValkeySentinel?>(ValkeySentinel::class.java)
        master1 = Mockito.mock<ValkeyStandalone?>(ValkeyStandalone::class.java)
        master2 = Mockito.mock<ValkeyStandalone?>(ValkeyStandalone::class.java)
    }

    @Test
    @Throws(IOException::class)
    fun testSimpleOperationsAfterRunWithSingleMasterNoSlavesCluster() {
        val cluster = ValkeyHighAvailability.builder()
            .withSentinelBuilder(sentinelBuilder)
            .sentinelCount(1)
            .replicationGroup("ourmaster", 0)
            .build()
        cluster.start()

        var pool: JedisSentinelPool? = null
        var jedis: Jedis? = null
        try {
            pool = JedisSentinelPool("ourmaster", Set.of<String?>("localhost:26379"))
            jedis = testPool(pool)
        } finally {
            if (jedis != null) {
                jedis.close()
            }
            if (pool != null) {
                pool.destroy()
            }
            cluster.stop()
        }
    }

    @Test
    @Throws(IOException::class)
    fun testSimpleOperationsAfterRunWithSingleMasterAndOneSlave() {
        val cluster = ValkeyHighAvailability.builder()
            .withSentinelBuilder(sentinelBuilder)
            .sentinelCount(1)
            .replicationGroup("ourmaster", 1)
            .build()
        cluster.start()

        var pool: JedisSentinelPool? = null
        var jedis: Jedis? = null
        try {
            pool = JedisSentinelPool("ourmaster", Set.of<String?>("localhost:26379"))
            jedis = testPool(pool)
        } finally {
            if (jedis != null) {
                jedis.close()
            }
            if (pool != null) {
                pool.destroy()
            }
            cluster.stop()
        }
    }

    @Test
    @Throws(IOException::class)
    fun testSimpleOperationsAfterRunWithSingleMasterMultipleSlaves() {
        val cluster = ValkeyHighAvailability.builder()
            .withSentinelBuilder(sentinelBuilder)
            .sentinelCount(1)
            .replicationGroup("ourmaster", 2)
            .build()
        cluster.start()

        var pool: JedisSentinelPool? = null
        var jedis: Jedis? = null
        try {
            pool = JedisSentinelPool("ourmaster", Set.of<String?>("localhost:26379"))
            jedis = testPool(pool)
        } finally {
            if (jedis != null) {
                jedis.close()
            }
            if (pool != null) {
                pool.destroy()
            }
            cluster.stop()
        }
    }

    @Test
    @Throws(IOException::class)
    fun testSimpleOperationsAfterRunWithTwoSentinelsSingleMasterMultipleSlaves() {
        val cluster = ValkeyHighAvailability.builder()
            .withSentinelBuilder(sentinelBuilder)
            .sentinelCount(2)
            .replicationGroup("ourmaster", 2)
            .build()
        cluster.start()

        var pool: JedisSentinelPool? = null
        var jedis: Jedis? = null
        try {
            pool = JedisSentinelPool("ourmaster", Set.of<String?>("localhost:26379", "localhost:26380"))
            jedis = testPool(pool)
        } finally {
            if (jedis != null) {
                jedis.close()
            }
            if (pool != null) {
                pool.destroy()
            }
            cluster.stop()
        }
    }

    @Test
    @Throws(IOException::class)
    fun testSimpleOperationsAfterRunWithTwoPredefinedSentinelsSingleMasterMultipleSlaves() {
        val cluster = ValkeyHighAvailability.builder()
            .withSentinelBuilder(sentinelBuilder)
            .replicationGroup("ourmaster", 2)
            .build()
        cluster.start()
        val hosts: MutableSet<String?> = HashSet()
        for (p in cluster.sentinelPorts()) {
            hosts.add("localhost:" + p)
        }
        val sentinelHosts = hosts

        var pool: JedisSentinelPool? = null
        var jedis: Jedis? = null
        try {
            pool = JedisSentinelPool("ourmaster", sentinelHosts)
            jedis = testPool(pool)
        } finally {
            if (jedis != null) {
                jedis.close()
            }
            if (pool != null) {
                pool.destroy()
            }
            cluster.stop()
        }
    }

    @Test
    @Throws(IOException::class)
    fun testSimpleOperationsAfterRunWithThreeSentinelsThreeMastersOneSlavePerMasterCluster() {
        val master1 = "master1"
        val master2 = "master2"
        val master3 = "master3"
        val cluster = ValkeyHighAvailability.builder()
            .withSentinelBuilder(sentinelBuilder)
            .sentinelCount(3)
            .quorumSize(2)
            .replicationGroup(master1, 1)
            .replicationGroup(master2, 1)
            .replicationGroup(master3, 1)
            .build()
        cluster.start()

        var pool1: JedisSentinelPool? = null
        var pool2: JedisSentinelPool? = null
        var pool3: JedisSentinelPool? = null
        var jedis1: Jedis? = null
        var jedis2: Jedis? = null
        var jedis3: Jedis? = null
        try {
            pool1 = JedisSentinelPool(
                master1,
                Set.of<String?>("localhost:26379", "localhost:26380", "localhost:26381")
            )
            pool2 = JedisSentinelPool(
                master2,
                Set.of<String?>("localhost:26379", "localhost:26380", "localhost:26381")
            )
            pool3 = JedisSentinelPool(
                master3,
                Set.of<String?>("localhost:26379", "localhost:26380", "localhost:26381")
            )
            jedis1 = testPool(pool1)
            jedis2 = testPool(pool2)
            jedis3 = testPool(pool3)
        } finally {
            if (jedis1 != null) {
                jedis1.close()
            }
            if (pool1 != null) {
                pool1.destroy()
            }
            if (jedis2 != null) {
                jedis2.close()
            }
            if (pool2 != null) {
                pool2.destroy()
            }
            if (jedis3 != null) {
                jedis3.close()
            }
            if (pool3 != null) {
                pool3.destroy()
            }
            cluster.stop()
        }
    }

    @Test
    @Throws(IOException::class)
    fun testSimpleOperationsAfterRunWithThreeSentinelsThreeMastersOneSlavePerMasterEphemeralCluster() {
        val master1 = "master1"
        val master2 = "master2"
        val master3 = "master3"
        val cluster = ValkeyHighAvailability.builder().withSentinelBuilder(sentinelBuilder)
            .sentinelCount(3).quorumSize(2)
            .replicationGroup(master1, 1)
            .replicationGroup(master2, 1)
            .replicationGroup(master3, 1)
            .build()
        cluster.start()
        val hosts: MutableSet<String?> = HashSet()
        for (p in cluster.sentinelPorts()) {
            hosts.add("localhost:" + p)
        }
        val sentinelHosts = hosts

        var pool1: JedisSentinelPool? = null
        var pool2: JedisSentinelPool? = null
        var pool3: JedisSentinelPool? = null
        var jedis1: Jedis? = null
        var jedis2: Jedis? = null
        var jedis3: Jedis? = null
        try {
            pool1 = JedisSentinelPool(master1, sentinelHosts)
            pool2 = JedisSentinelPool(master2, sentinelHosts)
            pool3 = JedisSentinelPool(master3, sentinelHosts)
            jedis1 = testPool(pool1)
            jedis2 = testPool(pool2)
            jedis3 = testPool(pool3)
        } finally {
            if (jedis1 != null) {
                jedis1.close()
            }
            if (pool1 != null) {
                pool1.destroy()
            }
            if (jedis2 != null) {
                jedis2.close()
            }
            if (pool2 != null) {
                pool2.destroy()
            }
            if (jedis3 != null) {
                jedis3.close()
            }
            if (pool3 != null) {
                pool3.destroy()
            }
            cluster.stop()
        }
    }

    private fun testPool(pool: JedisSentinelPool): Jedis {
        val jedis: Jedis
        jedis = pool.getResource()
        jedis.mset("abc", "1", "def", "2")

        Assert.assertEquals("1", jedis.mget("abc").get(0))
        Assert.assertEquals("2", jedis.mget("def").get(0))
        Assert.assertNull(jedis.mget("xyz").get(0))
        return jedis
    }
}
