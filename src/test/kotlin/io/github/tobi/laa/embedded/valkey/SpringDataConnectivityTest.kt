package io.github.tobi.laa.embedded.valkey

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone.Companion.builder
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import java.io.IOException

class SpringDataConnectivityTest {
    private var valkeyStandalone: ValkeyStandalone? = null
    private var template: RedisTemplate<String?, String?>? = null
    private var connectionFactory: JedisConnectionFactory? = null

    @Before
    @Throws(IOException::class)
    fun setUp() {
        valkeyStandalone = builder().port(6381).build()
        valkeyStandalone!!.start()

        connectionFactory = JedisConnectionFactory()
        connectionFactory!!.getStandaloneConfiguration()!!.setHostName("localhost")
        connectionFactory!!.getStandaloneConfiguration()!!.setPort(6381)
        connectionFactory!!.afterPropertiesSet()

        template = StringRedisTemplate()
        template!!.setConnectionFactory(connectionFactory)
        template!!.afterPropertiesSet()
    }

    @Test
    fun shouldBeAbleToUseSpringData() {
        template!!.opsForValue().set("foo", "bar")

        val result = template!!.opsForValue().get("foo")

        Assert.assertEquals("bar", result)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        valkeyStandalone!!.stop()
    }
}
