package io.github.tobi.laa.embedded.valkey.conf

import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import redis.embedded.RedisInstance
import java.nio.file.Paths

@DisplayName("Tests for ValkeyConfLocator")
@ExtendWith(MockKExtension::class)
internal class ValkeyConfLocatorTest {

    @Test
    @DisplayName("ValkeyConfLocator should throw exception for empty args")
    fun emptyArgs_shouldThrowException() {
        val valkey = TestValkey(emptyList())
        assertThatThrownBy { ValkeyConfLocator.locate(valkey) }
            .isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessage("No config file found for embedded Valkey server: $valkey")
    }

    @Test
    @DisplayName("ValkeyConfLocator should throw exception for missing file with .conf extension")
    fun missingFile_shouldThrowException() {
        val valkey = TestValkey(listOf("redis.properties"))
        assertThatThrownBy { ValkeyConfLocator.locate(valkey) }
            .isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessage("No config file found for embedded Valkey server: $valkey")
    }

    @Test
    @DisplayName("ValkeyConfLocator should return path to valkey.conf file if it exists")
    fun redisConfFound_shouldReturnPath() {
        val valkey = TestValkey(listOf("redis.conf"))
        assertThat(ValkeyConfLocator.locate(valkey)).isEqualTo(Paths.get("redis.conf"))
    }

    private class TestValkey(args: List<String>) : RedisInstance(0, args, null, false, null, null) {

        override fun start() {}
        override fun stop() {}
        override fun ports(): MutableList<Int> = mutableListOf()
        override fun active(): Boolean = false
    }
}