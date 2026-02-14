package io.github.tobi.laa.embedded.valkey.testing

import io.github.tobi.laa.embedded.valkey.installation.DistributionType
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallation
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSupplier
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import java.nio.file.Files
import java.nio.file.Path

fun createExecutableScript(contents: String): Path {
    val script = Files.createTempFile("valkey-script", ".sh")
    Files.writeString(script, contents)
    script.toFile().setExecutable(true)
    return script
}

fun createValkeyInstallation(binary: Path, operatingSystem: OperatingSystem = OperatingSystem.LINUX_X86_64): ValkeyInstallation {
    val installDir = Files.createTempDirectory("valkey-install")
    return ValkeyInstallation(
        version = "9.0.2",
        operatingSystem = operatingSystem,
        distributionType = DistributionType.VALKEY,
        installationPath = installDir,
        binaryPath = binary
    )
}

fun createInstallationSupplier(
    scriptContents: String,
    operatingSystem: OperatingSystem = OperatingSystem.LINUX_X86_64
): ValkeyInstallationSupplier {
    return ValkeyInstallationSupplier {
        createValkeyInstallation(createExecutableScript(scriptContents), operatingSystem)
    }
}
