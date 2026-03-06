package io.github.tobi.laa.embedded.valkey.conf

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Tests for ValkeyConfWriter")
class ValkeyConfWriterTest {

    @TempDir
    private lateinit var tempDir: Path

    private var givenConf: ValkeyConf? = null
    private var outputFile: Path? = null
    private var performAction: ThrowableAssert.ThrowingCallable? = null

    @BeforeEach
    fun reset() {
        givenConf = null
        outputFile = null
        performAction = null
    }

    @Test
    @DisplayName("Arguments containing no special characters should be written without quoting")
    fun `writes plain arguments without quoting`() {
        givenConf(ValkeyDirective("bind", "127.0.0.1"))
        givenOutputFile()
        whenWriteIsCalled()
        thenNoErrorOccurs()
        thenOutputContainsExactLines("bind 127.0.0.1")
    }

    @Test
    @DisplayName("Arguments containing whitespace should be wrapped in double quotes")
    fun `quotes arguments containing whitespace`() {
        givenConf(ValkeyDirective("requirepass", "my password"))
        givenOutputFile()
        whenWriteIsCalled()
        thenNoErrorOccurs()
        thenOutputContainsExactLines("""requirepass "my password"""")
    }

    @Test
    @DisplayName("Arguments containing double quotes should be wrapped in double quotes and have internal double quotes escaped")
    fun `quotes and escapes arguments containing double quotes`() {
        givenConf(ValkeyDirective("user", "name\"with-quote"))
        givenOutputFile()
        whenWriteIsCalled()
        thenNoErrorOccurs()
        thenOutputContainsExactLines("""user "name\"with-quote"""")
    }

    @Test
    @DisplayName("Arguments containing single quotes should be wrapped in double quotes")
    fun `quotes arguments containing single quotes`() {
        givenConf(ValkeyDirective("requirepass", "it's-a-secret"))
        givenOutputFile()
        whenWriteIsCalled()
        thenNoErrorOccurs()
        thenOutputContainsExactLines("""requirepass "it's-a-secret"""")
    }

    // --- given* ---

    private fun givenConf(vararg directives: ValkeyDirective) {
        givenConf = ValkeyConf(directives.toList())
    }

    private fun givenOutputFile() {
        outputFile = tempDir.resolve("valkey.conf")
    }

    // --- when* ---

    private fun whenWriteIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            ValkeyConfWriter.write(givenConf!!, outputFile!!)
        }
    }

    // --- then* ---

    private fun thenNoErrorOccurs() {
        assertThatCode(performAction!!).doesNotThrowAnyException()
    }

    private fun thenOutputContainsExactLines(vararg expectedLines: String) {
        assertThat(Files.readAllLines(outputFile!!)).containsExactly(*expectedLines)
    }
}
