package redis.embedded.resource

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.text.RegexOption.IGNORE_CASE


internal class TarGzipResourceSupplier(val tarPath: String, val resourceWithinTar: String) :
    ResourceSupplier {

    override fun supplyResource(targetDirectory: Path): Path {
        this.javaClass.getResourceAsStream(tarPath).use { resourceStream ->
            if (resourceStream == null) {
                throw FileNotFoundException("Could not find Tar archive at " + tarPath)
            }
            GzipCompressorInputStream(resourceStream).use { gzipStream ->
                TarArchiveInputStream(gzipStream).use {
                    extractTarGzip(it, targetDirectory)
                    var resource = targetDirectory.resolve(resourceWithinTar)
                    if (Files.notExists(resource)) {
                        throw FileNotFoundException("Could not find resource $resourceWithinTar in Tar archive $tarPath")
                    } else {
                        return resource
                    }
                }
            }
        }
    }

    internal fun extractTarGzip(tarStream: TarArchiveInputStream, targetDirectory: Path) {
        var entry: ArchiveEntry? = tarStream.nextEntry
        while (entry != null) {
            val extractTo: Path = targetDirectory.resolve(entry.getName())
            if (entry.isDirectory) {
                Files.createDirectories(extractTo)
            } else {
                Files.copy(tarStream, extractTo, REPLACE_EXISTING)
            }
            entry = tarStream.nextEntry
        }
    }

    override fun resourceName(): String {
        return tarPath.replaceFirst(Regex("\\.tar\\.gz$", IGNORE_CASE), "")
    }
}