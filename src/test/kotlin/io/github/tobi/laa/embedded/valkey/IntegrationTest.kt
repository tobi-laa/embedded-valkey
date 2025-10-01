package io.github.tobi.laa.embedded.valkey

import org.junit.jupiter.api.extension.ExtendWith

/**
 * Common annotation shared by all integration tests
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ExtendWith(ValkeyCleanupExtension::class)
annotation class IntegrationTest
