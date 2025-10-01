package redis.embedded;

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SpringDataConnectivityTest {

    private ValkeyStandalone valkeyStandalone;
    private RedisTemplate<String, String> template;
    private JedisConnectionFactory connectionFactory;

    @Before
    public void setUp() throws IOException {
        valkeyStandalone = ValkeyStandalone.builder().port(6381).build();
        valkeyStandalone.start();

        connectionFactory = new JedisConnectionFactory();
        connectionFactory.getStandaloneConfiguration().setHostName("localhost");
        connectionFactory.getStandaloneConfiguration().setPort(6381);
        connectionFactory.afterPropertiesSet();

        template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
    }

    @Test
    public void shouldBeAbleToUseSpringData() {
        template.opsForValue().set("foo", "bar");

        final String result = template.opsForValue().get("foo");

        assertEquals("bar", result);
    }

    @After
    public void tearDown() throws IOException {
        valkeyStandalone.stop();
    }

}
