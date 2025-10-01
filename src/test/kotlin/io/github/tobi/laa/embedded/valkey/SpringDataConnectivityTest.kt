package io.github.tobi.laa.embedded.valkey

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone.Companion.builder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate

@IntegrationTest
class SpringDataConnectivityTest {
    private var valkeyStandalone: ValkeyStandalone? = null
    private var template: RedisTemplate<String?, String?>? = null
    private var connectionFactory: JedisConnectionFactory? = null

    @BeforeEach
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

        Assertions.assertEquals("bar", result)
    }
}
