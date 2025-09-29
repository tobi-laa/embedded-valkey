package io.github.tobi.laa.embedded.valkey.distribution.bundle

/**
 * Returns the system temporary directory.
 *
 * @throws IllegalStateException if the system property 'java.io.tmpdir' is not set.
 * @return the system temporary directory.
 */
fun systemTempDirectory(): String =
    System.getProperty("java.io.tmpdir") ?: throw IllegalStateException("System property 'java.io.tmpdir' is not set")