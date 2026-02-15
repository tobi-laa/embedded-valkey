package io.github.tobi.laa.embedded.valkey

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone.Companion.builder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

@IntegrationTest
@DisplayName("Spring Data Redis connectivity tests")
class SpringDataConnectivityTest {
    private var valkeyStandalone: ValkeyStandalone? = null
    private var template: StringRedisTemplate? = null
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
    @DisplayName("It should be possible to read and write data using Spring Data Redis")
    fun shouldBeAbleToUseSpringData() {
        template!!.opsForValue().set("foo", "bar")

        val result = template!!.opsForValue().get("foo")

        Assertions.assertEquals("bar", result)
    }
}
