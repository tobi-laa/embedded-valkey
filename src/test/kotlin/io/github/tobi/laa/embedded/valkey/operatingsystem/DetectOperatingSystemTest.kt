package io.github.tobi.laa.embedded.valkey.operatingsystem

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream

@DisplayName("Tests for detectOperatingSystem")
class DetectOperatingSystemTest {

    @Test
    @DisplayName("Should throw OperatingSystemDetectionException when os.name property is missing")
    fun `detectOperatingSystem throws when os name missing`() {
        withOsName(null) {
            assertThatThrownBy { detectOperatingSystem() }
                .isInstanceOf(OperatingSystemDetectionException::class.java)
        }
    }

    @Test
    @DisplayName("Should throw UnsupportedOperatingSytemException for an unsupported OS name")
    fun `detectOperatingSystem rejects unsupported os`() {
        withOsName("Plan9") {
            assertThatThrownBy { detectOperatingSystem() }
                .isInstanceOf(UnsupportedOperatingSytemException::class.java)
        }
    }

    @Test
    @DisplayName("Should detect Linux x86_64 from uname output")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `detectOperatingSystem detects linux x86_64`() {
        withOsName("Linux") {
            withUnameOutput("x86_64") {
                assertThat(detectOperatingSystem()).isEqualTo(OperatingSystem.LINUX_X86_64)
            }
        }
    }

    @Test
    @DisplayName("Should detect Linux x86_64 when uname reports amd64")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `detectOperatingSystem detects linux amd64`() {
        withOsName("Linux") {
            withUnameOutput("amd64") {
                assertThat(detectOperatingSystem()).isEqualTo(OperatingSystem.LINUX_X86_64)
            }
        }
    }

    @Test
    @DisplayName("Should detect Linux ARM64 when uname reports aarch64")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `detectOperatingSystem detects linux arm64`() {
        withOsName("Linux") {
            withUnameOutput("aarch64") {
                assertThat(detectOperatingSystem()).isEqualTo(OperatingSystem.LINUX_ARM64)
            }
        }
    }

    @Test
    @DisplayName("Should throw UnsupportedOperatingSytemException for unsupported Linux architecture")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `detectOperatingSystem rejects unsupported linux arch`() {
        withOsName("Linux") {
            withUnameOutput("mips") {
                assertThatThrownBy { detectOperatingSystem() }
                    .isInstanceOf(UnsupportedOperatingSytemException::class.java)
            }
        }
    }

    @Test
    @DisplayName("Should detect Mac OS x86_64 from uname output")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `detectOperatingSystem detects mac os x86_64`() {
        withOsName("Mac OS X") {
            withUnameOutput("x86_64") {
                assertThat(detectOperatingSystem()).isEqualTo(OperatingSystem.MAC_OS_X86_64)
            }
        }
    }

    @Test
    @DisplayName("Should detect Mac OS ARM64 from uname output")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `detectOperatingSystem detects mac os arm64`() {
        withOsName("Mac OS X") {
            withUnameOutput("arm64") {
                assertThat(detectOperatingSystem()).isEqualTo(OperatingSystem.MAC_OS_ARM64)
            }
        }
    }

    @Test
    @DisplayName("Should throw UnsupportedOperatingSytemException for unsupported Mac OS architecture")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `detectOperatingSystem rejects unsupported mac os arch`() {
        withOsName("Mac OS X") {
            withUnameOutput("mips") {
                assertThatThrownBy { detectOperatingSystem() }
                    .isInstanceOf(UnsupportedOperatingSytemException::class.java)
            }
        }
    }

    @Test
    @DisplayName("Should detect Windows x86_64 on a Windows system")
    @EnabledOnOs(OS.WINDOWS)
    fun `detectOperatingSystem detects windows x64`() {
        assertThat(detectOperatingSystem()).isEqualTo(OperatingSystem.WINDOWS_X86_64)
    }

    @Test
    @DisplayName("Should throw OperatingSystemDetectionException when uname output is empty")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `detectOperatingSystem fails on empty uname output`() {
        withOsName("Linux") {
            withUnameOutput("") {
                assertThatThrownBy { detectOperatingSystem() }
                    .isInstanceOf(OperatingSystemDetectionException::class.java)
            }
        }
    }

    @Test
    @DisplayName("Should throw OperatingSystemDetectionException when uname returns multiple lines")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `detectOperatingSystem fails on multiple uname lines`() {
        withOsName("Linux") {
            withUnameOutput("x86_64\narm64\n") {
                assertThatThrownBy { detectOperatingSystem() }
                    .isInstanceOf(OperatingSystemDetectionException::class.java)
            }
        }
    }

    @Test
    @DisplayName("Should throw OperatingSystemDetectionException when uname command is unavailable")
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun `detectOperatingSystem fails when uname is missing`() {
        withOsName("Linux") {
            withUnameFailure(IllegalStateException("missing")) {
                assertThatThrownBy { detectOperatingSystem() }
                    .isInstanceOf(OperatingSystemDetectionException::class.java)
            }
        }
    }

    private fun withOsName(value: String?, block: () -> Unit) {
        val original = System.getProperty("os.name")
        if (value == null) {
            System.clearProperty("os.name")
        } else {
            System.setProperty("os.name", value)
        }
        try {
            block()
        } finally {
            if (original == null) {
                System.clearProperty("os.name")
            } else {
                System.setProperty("os.name", original)
            }
        }
    }

    private fun withUnameOutput(output: String, block: () -> Unit) {
        Mockito.mockConstruction(ProcessBuilder::class.java) { mock, _ ->
            Mockito.`when`(mock.start()).thenReturn(FakeProcess(output))
        }.use {
            block()
        }
    }

    private fun withUnameFailure(error: Exception, block: () -> Unit) {
        Mockito.mockConstruction(ProcessBuilder::class.java) { mock, _ ->
            Mockito.`when`(mock.start()).thenThrow(error)
        }.use {
            block()
        }
    }

    private class FakeProcess(output: String) : Process() {
        private val input = ByteArrayInputStream(output.toByteArray())

        override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()

        override fun getInputStream(): InputStream = input

        override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))

        override fun waitFor(): Int = 0

        override fun exitValue(): Int = 0

        override fun destroy() {
            // no-op
        }
    }
}
