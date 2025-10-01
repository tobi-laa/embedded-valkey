package io.github.tobi.laa.embedded.valkey.conf

import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.writer

/**
 * Writes a `valkey.conf` file to a given location.
 */
object ValkeyConfWriter {

    /**
     * Writes the given [valkeyConf] to the given [location].
     * @param valkeyConf the Valkey configuration to write
     * @param location the location to write the Valkey configuration to
     * @param charset the charset to use (default is UTF-8)
     * @throws IOException if the file could not be written
     */
    @Throws(IOException::class)
    fun write(valkeyConf: ValkeyConf, location: Path, charset: Charset = Charsets.UTF_8) {
        location.writer(charset).use { writer ->
            valkeyConf.directives.forEach { directive ->
                writer.write(directive.keyword)
                for (arg in directive.arguments) {
                    writer.write(" ")
                    writer.write(quoteAndEscapeArgument(arg))
                }
                writer.write(System.lineSeparator())
            }
        }
    }

    private fun quoteAndEscapeArgument(argument: String): String {
        val containsWhitespace = argument.any { it.isWhitespace() }
        val containsQuotes = argument.contains('"') || argument.contains('\'')
        return when {
            containsWhitespace || containsQuotes -> '"' + argument.replace("\"", "\\\"") + '"'
            else -> argument
        }
    }
}