package io.github.tobi.laa.embedded.valkey.conf

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files

@DisplayName("Tests for ValkeyConfWriter")
class ValkeyConfWriterTest {

    @Test
    @DisplayName("Arguments containing whitespace or quotes should be properly escaped")
    fun `writer quotes and escapes arguments when needed`() {
        val conf = ValkeyConf(
            listOf(
                ValkeyDirective("bind", "127.0.0.1"),
                ValkeyDirective("requirepass", "with space"),
                ValkeyDirective("user", "name\"with-quote")
            )
        )
        val tempFile = Files.createTempFile("valkey", ".conf")

        ValkeyConfWriter.write(conf, tempFile)

        val lines = Files.readAllLines(tempFile)
        assertEquals("bind 127.0.0.1", lines[0])
        assertEquals("requirepass \"with space\"", lines[1])
        assertEquals("user \"name\\\"with-quote\"", lines[2])
    }
}
