package io.github.tobi.laa.embedded.valkey.distribution.bundle

fun systemTempDirectory(): String =
    System.getProperty("java.io.tmpdir") ?: throw IllegalStateException("System property 'java.io.tmpdir' is not set")