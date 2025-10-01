package io.github.tobi.laa.embedded.valkey

import io.github.netmikey.logunit.api.LogCapturer
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.slf4j.event.Level

@ExtendWith(MockKExtension::class)
@DisplayName("Tests for stopSafely")
internal class StopSafelyTest {

    @RelaxedMockK
    private lateinit var valkey: Valkey

    @RegisterExtension
    val logs: LogCapturer =
        LogCapturer.create().captureForLogger("io.github.tobi.laa.embedded.valkey", Level.DEBUG)

    @Test
    @DisplayName("stopSafely() should call stop() on the given Valkey")
    fun stopSafely_shouldCallCloseOnGivenAutoCloseable() {
        stopSafely(valkey)
        verify { valkey.stop() }
        logs.assertDoesNotContain("Failed to stop Valkey $valkey")
    }

    @Test
    @DisplayName("stopSafely() should log an error if stop() on the given Valkey throws an exception")
    fun stopSafely_shouldLogErrorIfStopThrowsException() {
        val exception = RuntimeException("close() failed")
        every { valkey.stop() } throws exception
        stopSafely(valkey)
        verify { valkey.stop() }
        val logEvent = logs.assertContains("Failed to stop Valkey $valkey")
        assertThat(logEvent.throwable).isSameAs(exception)
    }
}