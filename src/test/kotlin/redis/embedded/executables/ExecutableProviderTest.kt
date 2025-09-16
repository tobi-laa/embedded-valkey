package redis.embedded.executables

import com.fasterxml.jackson.databind.ObjectMapper
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
                OS.WINDOWS -> null
                OS.MAC_OS_X -> null
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
            val githubClient = RestClient.builder().baseUrl("https://api.github.com").build()
            val response =
                githubClient.get().uri("/repos/valkey-io/valkey/releases/latest").retrieve().body(String::class.java)
            val version = ObjectMapper().readTree(response).get("tag_name").asText()!!
            return version
        }
    }
}