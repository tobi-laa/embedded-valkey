package io.github.tobi.laa.embedded.valkey.valkeypackage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FileChecksumMismatchExceptionTest {

    @Test
    fun `exception keeps message`() {
        val exception = FileChecksumMismatchException("checksum mismatch")

        assertEquals("checksum mismatch", exception.message)
    }
}
