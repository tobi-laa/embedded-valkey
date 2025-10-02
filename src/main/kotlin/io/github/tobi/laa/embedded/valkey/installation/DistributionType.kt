package io.github.tobi.laa.embedded.valkey.installation

import io.github.tobi.laa.embedded.valkey.installation.DistributionType.MEMURAI
import io.github.tobi.laa.embedded.valkey.installation.DistributionType.REDIS
import io.github.tobi.laa.embedded.valkey.installation.DistributionType.VALKEY


/**
 * Represents the type of Valkey installation.
 *
 * While [VALKEY] is the primary distribution, others like [REDIS] and [MEMURAI] are included should you choose to want
 * to use any of those distributions instead of Valkey.
 *
 * [MEMURAI] is furthermore included as - at the time of writing - it is the only distribution that supports Windows.
 */
@Suppress("kotlin:S1128") // imports are used in KDoc (and are repeatedly being re-added by the IDE if removed)
enum class DistributionType(val displayName: String) {
    VALKEY("Valkey"),
    REDIS("Redis"),
    MEMURAI("Memurai"),

    @Suppress("unused")
    MEMURAI_FOR_VALKEY("Memurai for Valkey")
}