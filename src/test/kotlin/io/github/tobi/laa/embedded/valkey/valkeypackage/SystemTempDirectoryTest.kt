package io.github.tobi.laa.embedded.valkey.valkeypackage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests for systemTempDirectory")
class SystemTempDirectoryTest {

    @Test
    @DisplayName("systemTempDirectory() should return the value of java.io.tmpdir")
    fun `systemTempDirectory returns java io tmpdir`() {
        val original = System.getProperty("java.io.tmpdir")
        System.setProperty("java.io.tmpdir", "/tmp")
        try {
            assertEquals("/tmp", systemTempDirectory())
        } finally {
            if (original == null) {
                System.clearProperty("java.io.tmpdir")
            } else {
                System.setProperty("java.io.tmpdir", original)
            }
        }
    }

    @Test
    @DisplayName("systemTempDirectory() should throw when java.io.tmpdir is not set")
    fun `systemTempDirectory throws when property missing`() {
        val original = System.getProperty("java.io.tmpdir")
        System.clearProperty("java.io.tmpdir")
        try {
            assertThrows(IllegalStateException::class.java) { systemTempDirectory() }
        } finally {
            if (original == null) {
                System.clearProperty("java.io.tmpdir")
            } else {
                System.setProperty("java.io.tmpdir", original)
            }
        }
    }
}
