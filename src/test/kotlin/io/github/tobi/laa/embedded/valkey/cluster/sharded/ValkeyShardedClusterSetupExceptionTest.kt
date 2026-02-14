package io.github.tobi.laa.embedded.valkey.cluster.sharded

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ValkeyShardedClusterSetupExceptionTest {

    @Test
    fun `exception keeps message`() {
        val exception = ValkeyShardedClusterSetupException("message")

        assertEquals("message", exception.message)
    }

    @Test
    fun `exception keeps cause`() {
        val cause = IllegalArgumentException("cause")
        val exception = ValkeyShardedClusterSetupException("message", cause)

        assertEquals("message", exception.message)
        assertSame(cause, exception.cause)
    }
}
