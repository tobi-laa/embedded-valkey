package io.github.tobi.laa.embedded.valkey.conf

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Tests for ValkeyConfBuilder")
class ValkeyConfBuilderTest {

    @Test
    @DisplayName("Calling binds() should replace previous bind directives")
    fun `binds replace previous bind directives`() {
        val builder = ValkeyConfBuilder()
            .binds("127.0.0.1")
            .binds("::1")

        val conf = builder.build()

        assertEquals(listOf("::1"), conf.binds())
    }

    @Test
    @DisplayName("Port validation should reject ports outside the valid range")
    fun `port validation rejects invalid ports`() {
        val builder = ValkeyConfBuilder()

        assertThrows(IllegalStateException::class.java) { builder.port(0) }
        assertThrows(IllegalStateException::class.java) { builder.port(70000) }
    }

    @Test
    @DisplayName("Setting port twice should override the previous value")
    fun `port overrides previous value`() {
        val conf = ValkeyConfBuilder().port(6379).port(6380).build()

        assertEquals(6380, conf.port())
    }

    @Test
    @DisplayName("Setting replicaOf twice should override the previous directive")
    fun `replicaof overrides previous directive`() {
        val conf = ValkeyConfBuilder()
            .replicaOf("host1", 6380)
            .replicaOf("host2", 6381)
            .build()

        val directive = conf.directives(ValkeyDirective.KEYWORD_REPLICAOF).single()
        assertEquals(listOf("host2", "6381"), directive.arguments)
    }

    @Test
    @DisplayName("port() should return null for a non-numeric port value")
    fun `port returns null for non numeric value`() {
        val conf = ValkeyConf(listOf(ValkeyDirective(ValkeyDirective.KEYWORD_PORT, "not-a-number")))

        assertNull(conf.port())
    }

    @Test
    @DisplayName("importConf() should append directives from the imported config")
    fun `importConf appends directives`() {
        val imported = ValkeyConfBuilder().port(6379).build()
        val conf = ValkeyConfBuilder()
            .binds("127.0.0.1")
            .importConf(imported)
            .build()

        assertEquals(listOf("127.0.0.1"), conf.binds())
        assertEquals(6379, conf.port())
    }

    @Test
    @DisplayName("DEFAULT_CONF should provide default bind and port")
    fun `default conf provides bind and port`() {
        val conf = ValkeyConf.DEFAULT_CONF

        assertEquals(listOf("::1"), conf.binds())
        assertEquals(6379, conf.port())
    }

    @Test
    @DisplayName("replicaOf port validation should reject ports outside the valid range")
    fun `replicaOf rejects invalid ports`() {
        val builder = ValkeyConfBuilder()

        assertThrows(IllegalStateException::class.java) { builder.replicaOf("localhost", 0) }
        assertThrows(IllegalStateException::class.java) { builder.replicaOf("localhost", 65536) }
    }

    @Test
    @DisplayName("port() should return null when no port directive exists")
    fun `port returns null when no port set`() {
        val conf = ValkeyConfBuilder().build()

        assertNull(conf.port())
    }

    @Test
    @DisplayName("ValkeyConfWriter.write without charset should use UTF-8 by default")
    fun `ValkeyConfWriter write without charset uses UTF-8`(@TempDir tempDir: Path) {
        val confFile = tempDir.resolve("test.conf")
        val conf = ValkeyConfBuilder().port(6380).build()
        ValkeyConfWriter.write(conf, confFile)
        assertThat(Files.readString(confFile)).contains("6380")
    }
}
