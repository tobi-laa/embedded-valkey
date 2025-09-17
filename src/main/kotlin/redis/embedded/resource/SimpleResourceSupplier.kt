package redis.embedded.resource

import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

internal class SimpleResourceSupplier(val path: String) : ResourceSupplier {

    override fun supplyResource(targetDirectory: Path): Path {
        this.javaClass.getResourceAsStream('/' + path).use {
            if (it == null) {
                throw FileNotFoundException("Could not find resource at " + path)
            }
            val targetPath = targetDirectory.resolve(resourceName())
            return Files.write(
                targetPath,
                it.readAllBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        }
    }

    override fun resourceName(): String {
        return path
    }
}