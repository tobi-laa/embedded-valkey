package io.github.tobi.laa.embedded.valkey.valkeypackage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests for FileChecksumMismatchException")
class FileChecksumMismatchExceptionTest {

    @Test
    @DisplayName("Exception should preserve its message")
    fun `exception keeps message`() {
        val exception = FileChecksumMismatchException("checksum mismatch")

        assertEquals("checksum mismatch", exception.message)
    }
}
