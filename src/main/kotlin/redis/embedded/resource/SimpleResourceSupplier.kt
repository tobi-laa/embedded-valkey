package redis.embedded.resource

import java.io.FileNotFoundException

internal class SimpleResourceSupplier(val path: String) : ResourceSupplier {

    override fun resolveResource(): ByteArray {
        this.javaClass.getResourceAsStream(path).use {
            if (it == null) throw FileNotFoundException("Could not find Redis executable at " + path)
            return it.readAllBytes()
        }
    }

    override fun resourceName(): String {
        return path
    }
}