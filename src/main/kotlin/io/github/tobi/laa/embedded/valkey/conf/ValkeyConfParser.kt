package io.github.tobi.laa.embedded.valkey.conf

import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.readLines

/**
 * Parses a `valkey.conf` file.
 */
object ValkeyConfParser {

    /**
     * Parses the given `valkey.conf` file.
     * @param file the `valkey.conf` file to parse
     * @return the parsed [ValkeyConf] file
     * @throws IllegalArgumentException if the file is not a valid `valkey.conf` file
     * @throws IOException if the file could not be read
     */
    @Throws(IOException::class)
    fun parse(file: Path): ValkeyConf {
        val directives = file
            .readLines()
            .map { it.trim() }
            .filterNot { it.isBlank() }
            .filterNot { isComment(it) }
            .map { parseDirective(it) }
        return ValkeyConf(directives)
    }

    private fun isComment(line: String): Boolean {
        return line.startsWith("#")
    }

    private fun parseDirective(line: String): Directive {
        line.split(Regex("\\s+"), 2).let {
            val keyword = it[0]
            val arguments = it.getOrNull(1) ?: throw IllegalArgumentException("No arguments found in line: '$line'")
            return Directive(keyword, parseArguments(arguments))
        }
    }

    private fun parseArguments(rawArguments: String): List<String> {
        val arguments = mutableListOf<String>()
        var state = ArgsParseState.UNESCAPED
        var currentArg = ""
        for (char in rawArguments) {
            when {
                char == '"' && state == ArgsParseState.UNESCAPED -> {
                    state = ArgsParseState.ESCAPED_DOUBLE
                }

                char == '\'' && state == ArgsParseState.UNESCAPED -> {
                    state = ArgsParseState.ESCAPED_SINGLE
                }

                char == '"' && state == ArgsParseState.ESCAPED_DOUBLE -> {
                    state = ArgsParseState.UNESCAPED
                }

                char == '\'' && state == ArgsParseState.ESCAPED_SINGLE -> {
                    state = ArgsParseState.UNESCAPED
                }

                char == ' ' && state == ArgsParseState.UNESCAPED -> {
                    arguments.add(currentArg)
                    currentArg = ""
                }

                else -> {
                    currentArg += char
                }
            }
        }
        when {
            state != ArgsParseState.UNESCAPED -> {
                throw IllegalArgumentException("Unbalanced quotes in arguments: '$rawArguments'")
            }

            else -> {
                arguments.add(currentArg)
                return arguments
            }
        }
    }

    internal enum class ArgsParseState {
        UNESCAPED, ESCAPED_SINGLE, ESCAPED_DOUBLE
    }
}