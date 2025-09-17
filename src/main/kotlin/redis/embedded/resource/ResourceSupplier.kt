package redis.embedded.resource

import java.io.IOException
import java.nio.file.Path

/**
 * Supplier of a resource bundled within a jar file, e.g. a Redis executable.
 */
interface ResourceSupplier {

    @Throws(IOException::class)
    fun supplyResource(targetDirectory: Path): Path

    fun resourceName(): String
}