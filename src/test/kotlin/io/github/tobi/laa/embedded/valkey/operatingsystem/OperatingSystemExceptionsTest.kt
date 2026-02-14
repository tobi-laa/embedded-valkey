package io.github.tobi.laa.embedded.valkey.operatingsystem

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class OperatingSystemExceptionsTest {

    @Test
    fun `detection exception carries message and cause`() {
        val cause = IllegalStateException("boom")
        val exception = OperatingSystemDetectionException("message", cause)

        assertEquals("message", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `unsupported exception keeps message`() {
        val exception = UnsupportedOperatingSytemException("unsupported")

        assertEquals("unsupported", exception.message)
    }
}
