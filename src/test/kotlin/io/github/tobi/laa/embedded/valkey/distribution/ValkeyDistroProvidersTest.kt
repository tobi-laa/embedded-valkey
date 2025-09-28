package io.github.tobi.laa.embedded.valkey.distribution

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.web.client.RestClient

@DisplayName("Tests for default Valkey distribution providers")
class ValkeyDistroProvidersTest {

    @Nested
    @DisplayName("Ensure that the latest Valkey (or Memurai) versions are provided by default")
    inner class LatestVersionsAreProvided {

        @DisplayName("Latest stable Valkey/Redis/Memurai version is provided")
        @ParameterizedTest(name = "Latest available stable version for OS/Architecture {0} is provided")
        @EnumSource(OperatingSystem::class)
        fun `latest available version is provided`(operatingSystem: OperatingSystem) {
            val providedVersion = DEFAULT_PROVIDERS[operatingSystem]!!.provideDistribution().version
            val newestAvailableVersion = when (operatingSystem) {
                WINDOWS_X86_64 -> identifyLatestAvailableMemuraiVersionOnNuget()
                MAC_OS_X86_64, MAC_OS_ARM64 -> identifyLatestAvailableMacportsValkeyVersion()
                LINUX_X86_64, LINUX_ARM64 -> identifyLatestAvailableValkeyVersion()
            }
            assert(providedVersion == newestAvailableVersion) {
                "\uD83D\uDC74 Provided version $providedVersion is not the latest available version $newestAvailableVersion for ${operatingSystem.displayName}"
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

        fun identifyLatestAvailableMemuraiVersionOnNuget(): String {
            return getMemuraiDeveloperNugetPage() //
                .select(".version-title") //
                .map { it.text().replace(Regex("^((:?[0-9]+\\.)+[0-9]+).*?$"), "$1") } //
                .first()
        }

        private fun getMemuraiDeveloperNugetPage(): Document =
            Jsoup.connect("https://www.nuget.org/packages/MemuraiDeveloper").get()

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