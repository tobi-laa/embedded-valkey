package redis.embedded.resource

import java.io.IOException

/**
 * Supplier of a resource bundled within a jar file, e.g. a Redis executable.
 */
@FunctionalInterface
interface ResourceSupplier {

    @Throws(IOException::class)
    fun resolveResource(): ByteArray

    fun resourceName(): String
}