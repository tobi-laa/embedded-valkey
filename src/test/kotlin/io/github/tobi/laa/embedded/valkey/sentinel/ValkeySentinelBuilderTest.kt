package io.github.tobi.laa.embedded.valkey.sentinel

import io.github.tobi.laa.embedded.valkey.cluster.highavailability.ReplicationGroup
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
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("Tests for ValkeySentinelBuilder")
class ValkeySentinelBuilderTest {

    companion object {
        private const val LOCALHOST_IPV4 = "127.0.0.1"
        private const val MONITOR = "monitor"
        private const val LOGLEVEL = "loglevel"
    }

    @TempDir
    private lateinit var tempDir: Path

    private var givenBuilder: ValkeySentinelBuilder? = null
    private var givenSentinel: ValkeySentinel? = null
    private var performAction: ThrowableAssert.ThrowingCallable? = null
    private var builtSentinel: ValkeySentinel? = null
    private var resolvedBuilder: ValkeySentinelBuilder? = null

    @BeforeEach
    fun reset() {
        givenBuilder = null
        givenSentinel = null
        performAction = null
        builtSentinel = null
        resolvedBuilder = null
    }

    @AfterEach
    fun cleanUpMocks() {
        unmockkAll()
    }

    @Test
    @DisplayName("Building should add a default monitor when no replication groups are configured")
    fun `build adds default monitor when no replication groups configured`() {
        givenBuilderWithDefaultSupplier()
        whenBuildIsCalled()
        thenNoErrorOccurs()
        thenBuiltSentinelConfigHasSentinelDirectiveContaining(MONITOR, "mymain", LOCALHOST_IPV4)
        thenBuiltSentinelConfigHasBinds("::1", LOCALHOST_IPV4)
        thenBuiltSentinelConfigHasPort()
    }

    @Test
    @DisplayName("Cloning should preserve replication groups and custom configuration")
    fun `clone preserves replication groups and configuration`() {
        givenBuilderWithDefaultSupplierAndConfig(bind = "0.0.0.0", port = 26390, quorumSize = 2)
        givenBuilderHasMonitor(ReplicationGroup("main", 6380, listOf(6381, 6382)))
        whenCloneAndBuildAreCalled()
        thenNoErrorOccurs()
        thenBuiltSentinelConfigHasSentinelDirectiveContaining(MONITOR, "main", LOCALHOST_IPV4, "6380", "2")
        thenBuiltSentinelConfigHasBinds("0.0.0.0")
        thenBuiltSentinelConfigHasPort(26390)
    }

    @Test
    @DisplayName("Building with replication groups should add monitors for each group")
    fun `build with replication groups adds monitors`() {
        givenBuilderWithDefaultSupplier()
        givenBuilderHasMonitor(ReplicationGroup("master1", 6380, listOf(6381)))
        givenBuilderHasMonitor(ReplicationGroup("master2", 6382, emptyList()))
        whenBuildIsCalled()
        thenNoErrorOccurs()
        thenBuiltSentinelConfigHasSentinelDirectiveContaining(MONITOR, "master1", LOCALHOST_IPV4, "6380")
        thenBuiltSentinelConfigHasSentinelDirectiveContaining(MONITOR, "master2", LOCALHOST_IPV4, "6382")
    }

    @Test
    @DisplayName("Builder setter methods should be chainable and their values reflected in the built sentinel configuration")
    fun `setter methods are chainable`() {
        givenBuilderWithDefaultSupplierAndSetters(downAfterMilliseconds = 5000, failOverTimeout = 10000, parallelSyncs = 2)
        whenBuildIsCalled()
        thenNoErrorOccurs()
        thenBuiltSentinelConfigHasSentinelDirectiveContaining("down-after-milliseconds", "mymain", "5000")
        thenBuiltSentinelConfigHasSentinelDirectiveContaining("failover-timeout", "mymain", "10000")
    }

    @Test
    @DisplayName("ValkeySentinel should report inactive when not started")
    fun `sentinel reports inactive when not started`() {
        givenBuiltSentinelWithDefaultSupplier()
        whenSentinelIsCheckedForActivity()
        thenNoErrorOccurs()
        thenSentinelIsInactive()
    }

    @Test
    @DisplayName("ValkeySentinel should throw an IllegalStateException with a 'Process not started' message when workingDirectory is accessed before starting")
    fun `sentinel throws on workingDirectory before start`() {
        givenBuiltSentinelWithDefaultSupplier()
        whenWorkingDirectoryIsAccessed()
        thenIllegalStateExceptionIsThrownContaining("Process not started")
    }

    @Test
    @DisplayName("ValkeySentinel stop should be safe to call when not yet started")
    fun `sentinel stop is safe when not started`() {
        givenBuiltSentinelWithDefaultSupplier()
        whenStopIsCalled()
        thenNoErrorOccurs()
    }

    @Test
    @DisplayName("ValkeySentinel companion builder method should return a ValkeySentinelBuilder instance")
    fun `companion builder returns builder`() {
        whenBuilderFactoryMethodIsCalled()
        thenNoErrorOccurs()
        thenResolvedBuilderIsValkeySentinelBuilder()
    }

    @Test
    @DisplayName("importConf(Path) should import the loglevel directive from a file")
    fun `importConf with path imports file`() {
        givenBuilderWithImportedConfPath()
        whenBuildIsCalled()
        thenNoErrorOccurs()
        thenBuiltSentinelConfigHasDirectiveContaining(LOGLEVEL, "notice")
    }

    @Test
    @DisplayName("importConf(String) should import the loglevel directive from a file path string")
    fun `importConf with string path imports file`() {
        givenBuilderWithImportedConfAsStringPath()
        whenBuildIsCalled()
        thenNoErrorOccurs()
        thenBuiltSentinelConfigHasDirectiveContaining(LOGLEVEL, "debug")
    }

    @Test
    @DisplayName("importConf(ValkeyConf) should import the loglevel directive from a ValkeyConf object")
    fun `importConf with ValkeyConf imports directives`() {
        givenBuilderWithImportedValkeyConf()
        whenBuildIsCalled()
        thenNoErrorOccurs()
        thenBuiltSentinelConfigHasDirectiveContaining(LOGLEVEL, "warning")
    }

    @Test
    @DisplayName("An IllegalStateException with a 'No installation supplier available' message should be thrown when no supplier is registered for the detected OS")
    fun `throws when no installation supplier available for OS`() {
        givenBuilderWithNoInstallationSupplierForCurrentOs()
        whenBuildIsCalled()
        thenIllegalStateExceptionIsThrownContaining("No installation supplier available for Linux for x86_64")
    }

    private fun givenBuilderWithDefaultSupplier() {
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeySentinelBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
    }

    private fun givenBuilderWithDefaultSupplierAndConfig(bind: String, port: Int, quorumSize: Int) {
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeySentinelBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
            .bind(bind)
            .port(port)
            .quorumSize(quorumSize)
    }

    private fun givenBuilderWithDefaultSupplierAndSetters(downAfterMilliseconds: Long, failOverTimeout: Long, parallelSyncs: Int) {
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeySentinelBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
            .downAfterMilliseconds(downAfterMilliseconds)
            .failOverTimeout(failOverTimeout)
            .parallelSyncs(parallelSyncs)
            .directive("loglevel", "debug")
    }

    private fun givenBuilderHasMonitor(group: ReplicationGroup) {
        givenBuilder!!.monitor(group)
    }

    private fun givenBuiltSentinelWithDefaultSupplier() {
        val operatingSystem = detectOperatingSystem()
        givenSentinel = ValkeySentinelBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
            .build()
    }

    private fun givenBuilderWithImportedConfPath() {
        val confFile = tempDir.resolve("sentinel.conf")
        Files.writeString(confFile, "loglevel notice" + System.lineSeparator())
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeySentinelBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
            .importConf(confFile)
    }

    private fun givenBuilderWithImportedConfAsStringPath() {
        val confFile = tempDir.resolve("sentinel.conf")
        Files.writeString(confFile, "loglevel debug" + System.lineSeparator())
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeySentinelBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
            .importConf(confFile.toString())
    }

    private fun givenBuilderWithImportedValkeyConf() {
        val conf: ValkeyConf = ValkeyConfBuilder().directive(LOGLEVEL, "warning").build()
        val operatingSystem = detectOperatingSystem()
        givenBuilder = ValkeySentinelBuilder()
            .installationSupplier(operatingSystem, createInstallationSupplier(ScriptBehavior.SLEEP_BRIEFLY, operatingSystem))
            .importConf(conf)
    }

    private fun givenBuilderWithNoInstallationSupplierForCurrentOs() {
        mockkStatic("io.github.tobi.laa.embedded.valkey.operatingsystem.DetectOperatingSystemKt")
        every { detectOperatingSystem() } returns OperatingSystem.LINUX_X86_64
        mockkStatic("io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt")
        every { DEFAULT_SUPPLIERS } returns emptyMap<OperatingSystem, ValkeyInstallationSupplier>()
        givenBuilder = ValkeySentinelBuilder()
    }

    private fun whenBuildIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            builtSentinel = givenBuilder!!.build()
        }
    }

    private fun whenCloneAndBuildAreCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            builtSentinel = givenBuilder!!.clone().build()
        }
    }

    private fun whenSentinelIsCheckedForActivity() {
        performAction = ThrowableAssert.ThrowingCallable { givenSentinel!!.active }
    }

    private fun whenWorkingDirectoryIsAccessed() {
        performAction = ThrowableAssert.ThrowingCallable { givenSentinel!!.workingDirectory }
    }

    private fun whenStopIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable { givenSentinel!!.stop() }
    }

    private fun whenBuilderFactoryMethodIsCalled() {
        performAction = ThrowableAssert.ThrowingCallable {
            resolvedBuilder = ValkeySentinel.builder()
        }
    }

    private fun thenNoErrorOccurs() {
        assertThatCode(performAction!!).doesNotThrowAnyException()
    }

    private fun thenBuiltSentinelConfigHasSentinelDirectiveContaining(vararg arguments: String) {
        assertThat(builtSentinel!!.config.directives("sentinel")).anySatisfy { directive ->
            assertThat(directive.arguments).contains(*arguments)
        }
    }

    private fun thenBuiltSentinelConfigHasDirectiveContaining(keyword: String, vararg arguments: String) {
        assertThat(builtSentinel!!.config.directives(keyword)).anySatisfy { directive ->
            assertThat(directive.arguments).contains(*arguments)
        }
    }

    private fun thenBuiltSentinelConfigHasBinds(vararg binds: String) {
        assertThat(builtSentinel!!.config.binds()).contains(*binds)
    }

    private fun thenBuiltSentinelConfigHasPort() {
        assertThat(builtSentinel!!.config.port()).isNotNull()
    }

    private fun thenBuiltSentinelConfigHasPort(expectedPort: Int) {
        assertThat(builtSentinel!!.config.port()).isEqualTo(expectedPort)
    }

    private fun thenSentinelIsInactive() {
        assertThat(givenSentinel!!.active).isFalse()
    }

    private fun thenResolvedBuilderIsValkeySentinelBuilder() {
        assertThat(resolvedBuilder).isInstanceOf(ValkeySentinelBuilder::class.java)
    }

    private fun thenIllegalStateExceptionIsThrownContaining(message: String) {
        assertThatCode(performAction!!).isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining(message)
    }
}
