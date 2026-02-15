package io.github.tobi.laa.embedded.valkey.testing

import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallation
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSupplier
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import java.nio.file.Files
import java.nio.file.Path

/**
 * Behaviors for test scripts that simulate various Valkey process states.
 */
enum class ScriptBehavior {
    SLEEP_BRIEFLY,
    ECHO_READY_AND_SLEEP,
    ECHO_SENTINEL_READY_AND_SLEEP,
    EXIT_IMMEDIATELY,
    IGNORE_TERM_AND_SLEEP,
    ECHO_READY_AND_STDERR_AND_SLEEP
}

fun isWindows(): Boolean = System.getProperty("os.name")?.lowercase()?.contains("windows") == true

@JvmOverloads
fun createExecutableScript(behavior: ScriptBehavior = ScriptBehavior.SLEEP_BRIEFLY): Path {
    return if (isWindows()) createWindowsScript(behavior) else createUnixScript(behavior)
}

@JvmOverloads
fun createValkeyInstallation(
    binary: Path,
    operatingSystem: OperatingSystem = detectOperatingSystem()
): ValkeyInstallation {
    val installDir = Files.createTempDirectory("valkey-install")
    return ValkeyInstallation(
        version = "9.0.2",
        operatingSystem = operatingSystem,
        distributionType = if (operatingSystem == OperatingSystem.WINDOWS_X86_64) DistributionType.MEMURAI else DistributionType.VALKEY,
        installationPath = installDir,
        binaryPath = binary
    )
}

@JvmOverloads
fun createInstallationSupplier(
    behavior: ScriptBehavior = ScriptBehavior.SLEEP_BRIEFLY,
    operatingSystem: OperatingSystem = detectOperatingSystem()
): ValkeyInstallationSupplier {
    return ValkeyInstallationSupplier {
        createValkeyInstallation(createExecutableScript(behavior), operatingSystem)
    }
}

private fun createUnixScript(behavior: ScriptBehavior): Path {
    val content = when (behavior) {
        ScriptBehavior.SLEEP_BRIEFLY ->
            "#!/bin/sh\nsleep 1\n"

        ScriptBehavior.ECHO_READY_AND_SLEEP ->
            "#!/bin/sh\necho \"Ready to accept connections\"\nwhile true; do sleep 1; done\n"

        ScriptBehavior.ECHO_SENTINEL_READY_AND_SLEEP ->
            "#!/bin/sh\necho \"Sentinel runid is 123\"\nwhile true; do sleep 1; done\n"

        ScriptBehavior.EXIT_IMMEDIATELY ->
            "#!/bin/sh\nexit 0\n"

        ScriptBehavior.IGNORE_TERM_AND_SLEEP ->
            "#!/bin/sh\ntrap '' TERM\nwhile true; do sleep 1; done\n"

        ScriptBehavior.ECHO_READY_AND_STDERR_AND_SLEEP ->
            "#!/bin/sh\necho \"Ready to accept connections\"\necho \"Some error message\" >&2\nwhile true; do sleep 1; done\n"
    }
    val script = Files.createTempFile("valkey-script", ".sh")
    Files.writeString(script, content)
    check(script.toFile().setExecutable(true)) { "Failed to make script executable: $script" }
    return script
}

private fun createWindowsScript(behavior: ScriptBehavior): Path {
    val content = when (behavior) {
        ScriptBehavior.SLEEP_BRIEFLY ->
            "@echo off\r\nping -n 2 127.0.0.1 >nul\r\n"

        ScriptBehavior.ECHO_READY_AND_SLEEP ->
            "@echo off\r\necho Ready to accept connections\r\n:loop\r\nping -n 2 127.0.0.1 >nul\r\ngoto loop\r\n"

        ScriptBehavior.ECHO_SENTINEL_READY_AND_SLEEP ->
            "@echo off\r\necho Sentinel runid is 123\r\n:loop\r\nping -n 2 127.0.0.1 >nul\r\ngoto loop\r\n"

        ScriptBehavior.EXIT_IMMEDIATELY ->
            "@echo off\r\nexit /b 0\r\n"

        ScriptBehavior.IGNORE_TERM_AND_SLEEP ->
            "@echo off\r\n:loop\r\nping -n 2 127.0.0.1 >nul\r\ngoto loop\r\n"

        ScriptBehavior.ECHO_READY_AND_STDERR_AND_SLEEP ->
            "@echo off\r\necho Ready to accept connections\r\necho Some error message 1>&2\r\n:loop\r\nping -n 2 127.0.0.1 >nul\r\ngoto loop\r\n"
    }
    val script = Files.createTempFile("valkey-script", ".bat")
    Files.writeString(script, content)
    return script
}
