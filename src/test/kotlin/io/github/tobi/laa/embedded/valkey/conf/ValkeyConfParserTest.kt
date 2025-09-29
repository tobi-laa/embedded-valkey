package io.github.tobi.laa.embedded.valkey.conf

import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions.*
import org.assertj.core.api.ThrowableAssert.ThrowingCallable
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.support.ParameterDeclarations
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.io.path.listDirectoryEntries

@DisplayName("Tests for ValkeyConfParser")
internal class ValkeyConfParserTest {

    private var givenFile: Path? = null
    private var parse: ThrowingCallable? = null
    private var result: ValkeyConf? = null

    @DisplayName("Official self documented examples for Valkey should be parsed without error")
    @ParameterizedTest(name = "{0} should be parsed without error")
    @ArgumentsSource(OfficialRedisExamples::class)
    fun officialSelfDocumentedValkeyExample_whenParsing_shouldSucceed(file: Path) {
        givenFile(file)
        whenParsing()
        thenValidValkeyConfShouldBeReturned()
    }

    @DisplayName("Official self documented examples for Redis should be parsed without error")
    @ParameterizedTest(name = "{0} should be parsed without error")
    @ArgumentsSource(OfficialRedisExamples::class)
    fun officialSelfDocumentedRedisExample_whenParsing_shouldSucceed(file: Path) {
        givenFile(file)
        whenParsing()
        thenValidValkeyConfShouldBeReturned()
    }

    // see also https://docs.memurai.com/en/config-file#default-configuration
    @DisplayName("Official default configuration for Memurai should be parsed without error")
    @Test
    fun officialDefaultMemuraiConf_whenParsing_shouldSucceed() {
        givenDefaultMemuraiConf()
        whenParsing()
        thenValidValkeyConfShouldBeReturned()
    }

    @DisplayName("Invalid valkey.conf should yield error when being parsed")
    @ParameterizedTest(name = "{0} should yield error message -> {1}")
    @ArgumentsSource(InvalidExamples::class)
    fun invalidExample_whenParsing_shouldYieldError(file: Path, expectedMsg: String) {
        givenFile(file)
        whenParsing()
        thenErrorShouldBeThrown().message().contains(expectedMsg)
    }

    @DisplayName("Valid valkey.conf should be parsed correctly with expected directives")
    @ParameterizedTest(name = "{0} should be parsed without error and yield expected ValkeyConf")
    @ArgumentsSource(ValidExamples::class)
    fun validExample_whenParsing_shouldYieldExpectedConf(file: Path, expected: ValkeyConf) {
        givenFile(file)
        whenParsing()
        thenParsedConfShouldBeAs(expected)
    }

    private fun givenDefaultMemuraiConf() {
        givenFile = Paths.get("src/test/resources/valkey-conf/official-memurai-examples/default_memurai.conf")
    }

    private fun givenFile(file: Path) {
        givenFile = file
    }

    private fun whenParsing() {
        parse = ThrowingCallable { result = ValkeyConfParser.parse(givenFile!!) }
    }

    private fun thenValidValkeyConfShouldBeReturned() {
        assertThatCode(parse).doesNotThrowAnyException()
        assertThat(result).isNotNull
        assertThat(result!!.directives).isNotEmpty
    }

    private fun thenParsedConfShouldBeAs(expected: ValkeyConf) {
        assertThatCode(parse!!).doesNotThrowAnyException()
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    private fun thenErrorShouldBeThrown(): AbstractThrowableAssert<*, *> {
        return assertThatThrownBy { parse!!.call() }.isExactlyInstanceOf(IllegalArgumentException::class.java)
    }

    internal class OfficialValkeyExamples : ArgumentsProvider {

        private val dir = Paths.get("src/test/resources/valkey-conf/official-valkey-examples")

        override fun provideArguments(
            parameters: ParameterDeclarations,
            extensionContext: ExtensionContext?
        ): Stream<Arguments> {
            val versions = dir.listDirectoryEntries().map { it.fileName }
            return versions
                .map {
                    arguments(
                        named(
                            "Example for version $it",
                            dir.resolve(it).resolve("valkey.conf")
                        )
                    )
                }
                .stream()
        }
    }

    internal class OfficialRedisExamples : ArgumentsProvider {

        private val dir = Paths.get("src/test/resources/valkey-conf/official-redis-examples")

        override fun provideArguments(
            parameters: ParameterDeclarations,
            extensionContext: ExtensionContext?
        ): Stream<Arguments> {
            val versions = dir.listDirectoryEntries().map { it.fileName }
            return versions
                .map {
                    arguments(
                        named(
                            "Example for version $it",
                            dir.resolve(it).resolve("redis.conf")
                        )
                    )
                }
                .stream()
        }
    }

    internal class InvalidExamples : ArgumentsProvider {

        private val dir = Paths.get("src/test/resources/valkey-conf/invalid-examples")

        override fun provideArguments(
            parameters: ParameterDeclarations,
            extensionContext: ExtensionContext?
        ): Stream<Arguments> {
            return Stream.of(
                arguments(
                    named(
                        "Conf with argument has unbalanced double quote",
                        dir.resolve("argument-unbalanced-double-quote.conf")
                    ),
                    "Unbalanced quotes in arguments: '6379\"'"
                ),
                arguments(
                    named(
                        "Conf with argument has unbalanced single quote",
                        dir.resolve("argument-unbalanced-single-quote.conf")
                    ),
                    "Unbalanced quotes in arguments: ''6379'"
                ),
                arguments(
                    named(
                        "Conf with keyword has double quotes",
                        dir.resolve("double-quoted-keyword.conf")
                    ),
                    "Keyword '\"port\"' contains illegal characters. Only alphanumeric characters, hyphens and underscores are allowed"
                ),
                arguments(
                    named(
                        "Conf with keyword has single quotes",
                        dir.resolve("single-quoted-keyword.conf")
                    ),
                    "Keyword ''port'' contains illegal characters. Only alphanumeric characters, hyphens and underscores are allowed"
                ),
                arguments(
                    named(
                        "Conf with keyword that contains hash",
                        dir.resolve("keyword-contains-hash.conf")
                    ),
                    "Keyword 'po#rt' contains illegal characters. Only alphanumeric characters, hyphens and underscores are allowed"
                ),
                arguments(
                    named(
                        "Conf with missing arguments",
                        dir.resolve("missing-arguments.conf")
                    ),
                    "No arguments found in line: 'port'"
                )
            )
        }
    }

    internal class ValidExamples : ArgumentsProvider {

        private val dir = Paths.get("src/test/resources/valkey-conf/valid-examples")

        override fun provideArguments(
            parameters: ParameterDeclarations,
            extensionContext: ExtensionContext?
        ): Stream<Arguments> {
            return Stream.of(
                arguments(
                    named(
                        "Empty config file",
                        dir.resolve("empty.conf")
                    ),
                    ValkeyConf(emptyList())
                ),
                arguments(
                    named(
                        "Conf with duplicate keywords",
                        dir.resolve("with-duplicate-keywords.conf")
                    ),
                    ValkeyConf(
                        listOf(
                            Directive("bind", "localhost"),
                            Directive("bind", "127.0.0.1", "::1"),
                            Directive("port", "6379")
                        )
                    )
                ),
                arguments(
                    named(
                        "Conf with escaped arguments",
                        dir.resolve("with-escaped-arguments.conf")
                    ),
                    ValkeyConf(
                        listOf(
                            Directive("tls-protocols", "TLSv1.2 TLSv1.3"),
                            Directive("logfile", ""),
                            Directive("proc-title-template", "{title} {listen-addr} {server-mode}"),
                            Directive("sentinel", "monitor", "Blue-lored Antbird", "::1", "6379", "1"),
                            Directive("double-quote", "\""),
                            Directive("single-quote", "'"),
                        )
                    )
                ),
                arguments(
                    named(
                        "Conf without duplicate keywords",
                        dir.resolve("without-duplicate-keywords.conf")
                    ),
                    ValkeyConf(
                        listOf(
                            Directive("bind", "i.like.trains.org"),
                            Directive("port", "6379")
                        )
                    )
                )
            )
        }
    }

    @Nested
    @DisplayName("ValkeyConfParser.ArgsParseState tests")
    internal inner class ArgsParseStateTest {

        @Test
        @DisplayName("Static field entries returns all enum values")
        fun getEntries_returnsAllValues() {
            assertThat(ValkeyConfParser.ArgsParseState.entries).containsExactlyInAnyOrder(
                ValkeyConfParser.ArgsParseState.UNESCAPED,
                ValkeyConfParser.ArgsParseState.ESCAPED_SINGLE,
                ValkeyConfParser.ArgsParseState.ESCAPED_DOUBLE
            )
        }
    }
}