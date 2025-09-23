package redis.embedded.executables

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.client.RestClient
import redis.embedded.model.OS
import redis.embedded.model.OsArchitecture


@DisplayName("Tests for ExecutableProvider")
class ExecutableProviderTest {

    @Nested
    @DisplayName("Ensure that the latest Redis versions are provided by this project")
    inner class LatestRedisVersionsAreProvided {

        @DisplayName("Latest stable Valkey/Redis/Memurai version is provided")
        @ParameterizedTest(name = "Latest available stable version for OS/Architecture {0} is provided")
        @EnumSource(OsArchitecture::class)
        fun `latest available version is provided`(osArchitecture: OsArchitecture) {
            val providedVersion = PROVIDED_VERSIONS[osArchitecture]!!.resourceName()
                .replace(Regex("^.*?((:?[0-9]+\\.)+[0-9]+).*?$"), "$1")
            val newestAvailableVersion = when (osArchitecture.os) {
                OS.WINDOWS -> identifyLatestAvailableMemuraiForValkeyVersion()
                OS.MAC_OS_X -> identifyLatestAvailableMacportsValkeyVersion()
                OS.UNIX -> identifyLatestAvailableValkeyVersion()
            }
            if (newestAvailableVersion == null) {
                println("\uD83E\uDD14 No version information available for ${osArchitecture.os}, skipping test for now.")
            } else {
                assert(providedVersion == newestAvailableVersion) {
                    "\uD83D\uDC74 Provided version $providedVersion is not the latest available version $newestAvailableVersion for ${osArchitecture.os}"
                }
            }
        }

        fun identifyLatestAvailableValkeyVersion(): String {
            val githubClientBuilder = RestClient.builder().baseUrl("https://api.github.com")
            val token = System.getenv("GITHUB_TOKEN")
            if (token != null) {
                githubClientBuilder.defaultHeader("Authorization", "Bearer $token")
            }
            val githubClient = githubClientBuilder.build()
            val response =
                githubClient.get().uri("/repos/valkey-io/valkey/releases").retrieve().body(String::class.java)
            val jsonArray = ObjectMapper().readTree(response) as ArrayNode
            return jsonArray.valueStream().map { it.get("tag_name").asText()!! }
                .filter { !it.contains("rc") && !it.contains("beta") && !it.contains("alpha") }
                .max(semanticVersionComparator)
                .orElseThrow { IllegalStateException("No releases found on GitHub") }
        }

        internal val semanticVersionComparator: Comparator<String> = Comparator { v1, v2 ->
            val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
            val length = maxOf(parts1.size, parts2.size)
            for (i in 0 until length) {
                val part1 = if (i < parts1.size) parts1[i] else 0
                val part2 = if (i < parts2.size) parts2[i] else 0
                if (part1 != part2) {
                    return@Comparator part1 - part2
                }
            }
            0
        }

        fun identifyLatestAvailableMemuraiForValkeyVersion(): String {
            return getDownloadMemuraiForValkeyPage() //
                .select("#__NEXT_DATA__") //
                .map { extractMemurayValkeyWindowsVersion(it.html()) } //
                .first()
        }

        private fun getDownloadMemuraiForValkeyPage(): Document =
            Jsoup.connect("https://www.memurai.com/thanks-for-downloading?version=windows-valkey").get()

        private fun extractMemurayValkeyWindowsVersion(nextDataJson: String): String =
            ObjectMapper().readTree(nextDataJson).get("props")
                .get("pageProps")
                .get("variables")
                .get("MEMURAI_VALKEY_WINDOWS_VERSION_SHORT")
                .asText()

        fun identifyLatestAvailableMacportsValkeyVersion(): String {
            return Jsoup.connect("https://packages.macports.com/valkey/") //
                .get() //
                .select("a") //
                .map { it.attribute("href")!!.value }
                .map { it.replace(Regex("^valkey-((:?[0-9]+\\.)+[0-9]+).*?$"), "$1") }
                .maxWith(semanticVersionComparator)
        }
    }
}