package redis.embedded.resource

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.FileNotFoundException
import kotlin.text.RegexOption.IGNORE_CASE


internal class TarGzipResourceSupplier(val tarPath: String, val resourceWithinTar: String) :
    ResourceSupplier {

    override fun resolveResource(): ByteArray {
        this.javaClass.getResourceAsStream(tarPath).use { resourceStream ->
            if (resourceStream == null) throw FileNotFoundException("Could not find Tar archive at " + tarPath)
            GzipCompressorInputStream(resourceStream).use { gzipStream ->
                TarArchiveInputStream(gzipStream).use {
                    return findResourceInTar(it)
                }
            }
        }
    }

    internal fun findResourceInTar(tarStream: TarArchiveInputStream): ByteArray {
        var entry: ArchiveEntry? = tarStream.nextEntry
        while (entry != null) {
            if (entry.name == resourceWithinTar) {
                return tarStream.readAllBytes()
            }
            entry = tarStream.nextEntry
        }
        throw FileNotFoundException("Could not find resource $resourceWithinTar in Tar archive $tarPath")
    }

    override fun resourceName(): String {
        return tarPath.replaceFirst(Regex("\\.tar\\.gz$", IGNORE_CASE), "")
    }
}