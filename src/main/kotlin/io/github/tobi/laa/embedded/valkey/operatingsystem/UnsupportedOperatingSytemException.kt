package io.github.tobi.laa.embedded.valkey.operatingsystem

/**
 * Exception to be thrown when an unsupported operating system is encountered.
 */
class UnsupportedOperatingSytemException(message: String) : OperatingSystemDetectionException(message)