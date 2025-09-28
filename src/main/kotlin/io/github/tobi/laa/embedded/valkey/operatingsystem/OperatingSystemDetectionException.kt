package io.github.tobi.laa.embedded.valkey.operatingsystem

/**
 * Exception to be thrown when an error occurs during operating system detection.
 * @param message the detail message
 * @param cause the cause of the exception
 */
open class OperatingSystemDetectionException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}