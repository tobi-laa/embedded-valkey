package io.github.tobi.laa.embedded.valkey.conf

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests for ValkeyConf")
internal class ValkeyConfTest {

    @Test
    @DisplayName("ValkeyConf should throw exception for blank keyword")
    fun blankKeyword_shouldThrowException() {
        assertThatThrownBy { ValkeyConf(listOf(Directive(""))) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Keyword must not be blank")
    }

    @Test
    @DisplayName("ValkeyConf should throw exception for missing argument")
    fun missingArgument_shouldThrowException() {
        assertThatThrownBy { ValkeyConf(listOf(Directive("dummy"))) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("At least one argument is required")
    }
}