package io.github.tobi.laa.embedded.valkey.valkeypackage

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests for systemTempDirectory")
class SystemTempDirectoryTest {

    private var originalTmpDir: String? = null

    @BeforeEach
    fun saveOriginalTmpDir() {
        originalTmpDir = System.getProperty("java.io.tmpdir")
    }

    @AfterEach
    fun restoreOriginalTmpDir() {
        if (originalTmpDir == null) {
            System.clearProperty("java.io.tmpdir")
        } else {
            System.setProperty("java.io.tmpdir", originalTmpDir!!)
        }
    }

    @Test
    @DisplayName("systemTempDirectory() should return the value of java.io.tmpdir")
    fun `systemTempDirectory returns java io tmpdir`() {
        System.setProperty("java.io.tmpdir", "/tmp")
        assertThat(systemTempDirectory()).isEqualTo("/tmp")
    }

    @Test
    @DisplayName("systemTempDirectory() should throw when java.io.tmpdir is not set")
    fun `systemTempDirectory throws when property missing`() {
        System.clearProperty("java.io.tmpdir")
        assertThatThrownBy { systemTempDirectory() }.isInstanceOf(IllegalStateException::class.java)
    }
}
