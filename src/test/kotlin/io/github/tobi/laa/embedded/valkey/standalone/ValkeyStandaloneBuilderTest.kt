package io.github.tobi.laa.embedded.valkey.standalone

import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder
import io.github.tobi.laa.embedded.valkey.installation.DEFAULT_SUPPLIERS
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSupplier
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import io.github.tobi.laa.embedded.valkey.operatingsystem.detectOperatingSystem
import io.github.tobi.laa.embedded.valkey.testing.ScriptBehavior
import io.github.tobi.laa.embedded.valkey.testing.createInstallationSupplier
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Tests for ValkeyStandaloneBuilder")
class ValkeyStandaloneBuilderTest {

    @TempDir
    private lateinit var tempDir: Path

    private var givenBuilder: ValkeyStandaloneBuilder? = null
    private var performAction: ThrowableAssert.ThrowingCallable? = null
    private var builtServer: ValkeyStandalone? = null
    private var resolvedBuilder: ValkeyStandaloneBuilder? = null

    @BeforeEach
    fun reset() {
        givenBuilder = null
        performAction = null
        builtServer = null
        resolvedBuilder = null
    }

    @AfterEach
    fun cleanUpMocks() {
        unmockkAll()
    }

    @Test
    @DisplayName("Building should apply a default port and bind addresses when none are configured")
    fun `build applies default port and binds`() {
        givenBuilderWithDefaultSupplier()
        whenBuildIsCalled()
        thenNoErrorOccurs()
        thenBuiltServerHasPort()
        thenBuiltServerHasBinds("::1", "127.0.0.1")
    }

    @Test
    @DisplayName("Cloning should preserve custom installation suppliers and configuration")
    fun `clone preserves configuration`() {
        givenBuilderWithDefaultSupplierAndConfig(bind = "0.0.0.0", port = 6390, directive = "maxmemory" to "1mb")
        whenCloneAndBuildAreCalled()
        thenNoErrorOccurs()
        thenBuiltServerHasPort(6390)
        thenBuiltServerHasBinds("0.0.0.0")
        thenBuiltServerHasDirective("maxmemory")
    }

    @Test
    @DisplayName("Building should include the replicaOf directive when configured")
    fun `build applies replicaOf`() {
        givenBuilderWithDefaultSupplierAndReplicaOf(hostname = "localhost", port = 6379)
        whenBuildIsCalled()
        thenNoErrorOccurs()
        thenBuiltServerHasDirective("replicaof")
    }

    @Test
    @DisplayName("importConf(Path) should apply port configuration from a file")
    fun `importConf with path applies configured port`() {
        givenBuilderWithImportedConfFile(port = 6399)
        whenBuildIsCalled()
        thenNoErrorOccurs()
        thenBuiltServerHasPort(6399)
    }

    @Test
    @DisplayName("importConf(ValkeyConf) should apply the configured port from a ValkeyConf object")
    fun `importConf with ValkeyConf applies configured port`() {
        givenBuilderWithImportedValkeyConf(port = 6398)
        whenBuildIsCalled()
        thenNoErrorOccurs()
        thenBuiltServerHasPort(6398)
    }

    @Test
    @DisplayName("ValkeyStandalone companion builder() method should return a ValkeyStandaloneBuilder instance")
    fun `companion builder returns builder`() {
        whenBuilderFactoryMethodIsCalled()
        thenNoErrorOccurs()
        thenResolvedBuilderIsValkeyStandaloneBuilder()
    }

    @Test
    @DisplayName("An IllegalStateException containing 'No installation supplier available' should be thrown when no supplier is registered for the detected OS")
    fun `throws when no installation supplier available for OS`() {
        givenBuilderWithNoInstallationSupplierForCurrentOs()
        whenBuildIsCalled()
        thenIllegalStateExceptionIsThrownContaining("No installation supplier available for Linux for x86_64")
    }

    // --- given* ---

    private fun givenBuilderWithDefaultSupplier() {
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeyStandaloneBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
    }

    private fun givenBuilderWithDefaultSupplierAndConfig(bind: String, port: Int, directive: Pair<String, String>) {
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeyStandaloneBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
            .bind(bind)
            .port(port)
            .directive(directive.first, directive.second)
    }

    private fun givenBuilderWithDefaultSupplierAndReplicaOf(hostname: String, port: Int) {
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeyStandaloneBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
            .replicaOf(hostname, port)
    }

    private fun givenBuilderWithImportedConfFile(port: Int) {
        val confFile = tempDir.resolve("test.conf")
        Files.writeString(confFile, "port $port" + System.lineSeparator())
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeyStandaloneBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
            .importConf(confFile)
    }

    private fun givenBuilderWithImportedValkeyConf(port: Int) {
        val conf: ValkeyConf = ValkeyConfBuilder().port(port).build()
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeyStandaloneBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
            .importConf(conf)
    }

    private fun givenBuilderWithNoInstallationSupplierForCurrentOs() {
        mockkStatic("io.github.tobi.laa.embedded.valkey.operatingsystem.DetectOperatingSystemKt")
        every { detectOperatingSystem() } returns OperatingSystem.LINUX_X86_64
        mockkStatic("io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt")
        every { DEFAULT_SUPPLIERS } returns emptyMap<OperatingSystem, ValkeyInstallationSupplier>()
        givenBuilder = ValkeyStandaloneBuilder()
    }

    // --- when* ---

    private fun whenBuildIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            builtServer = givenBuilder!!.build()
        }
    }

    private fun whenCloneAndBuildAreCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            builtServer = givenBuilder!!.clone().build()
        }
    }

    private fun whenBuilderFactoryMethodIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            resolvedBuilder = ValkeyStandalone.builder()
        }
    }

    // --- then* ---

    private fun thenNoErrorOccurs() {
        assertThatCode(performAction!!).doesNotThrowAnyException()
    }

    private fun thenIllegalStateExceptionIsThrownContaining(message: String) {
        assertThatCode(performAction!!).isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining(message)
    }

    private fun thenBuiltServerHasPort() {
        assertThat(builtServer!!.config.port()).isNotNull()
    }

    private fun thenBuiltServerHasPort(expectedPort: Int) {
        assertThat(builtServer!!.config.port()).isEqualTo(expectedPort)
    }

    private fun thenBuiltServerHasBinds(vararg binds: String) {
        assertThat(builtServer!!.config.binds()).contains(*binds)
    }

    private fun thenBuiltServerHasDirective(keyword: String) {
        assertThat(builtServer!!.config.directives(keyword)).isNotEmpty()
    }

    private fun thenResolvedBuilderIsValkeyStandaloneBuilder() {
        assertThat(resolvedBuilder).isInstanceOf(ValkeyStandaloneBuilder::class.java)
    }
}
