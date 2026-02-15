package io.github.tobi.laa.embedded.valkey.operatingsystem

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests for operating system detection exceptions")
class OperatingSystemExceptionsTest {

    @Test
    @DisplayName("OperatingSystemDetectionException should carry message and cause")
    fun `detection exception carries message and cause`() {
        val cause = IllegalStateException("boom")
        val exception = OperatingSystemDetectionException("message", cause)

        assertEquals("message", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    @DisplayName("UnsupportedOperatingSytemException should preserve its message")
    fun `unsupported exception keeps message`() {
        val exception = UnsupportedOperatingSytemException("unsupported")

        assertEquals("unsupported", exception.message)
    }
}
