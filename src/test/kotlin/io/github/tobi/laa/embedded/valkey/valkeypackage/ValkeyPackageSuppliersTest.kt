package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.netmikey.logunit.api.LogCapturer
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.*
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.support.ParameterDeclarations
import org.slf4j.event.Level
import java.io.IOException
import java.io.InputStream
import java.net.*
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

@DisplayName("Tests for Valkey package suppliers provided by this library")
class ValkeyPackageSuppliersTest {

    @Nested
    @DisplayName("Tests for default, classpath-based package suppliers")
    inner class ClasspathPackageSuppliers {

        private var givenOperatingSystem: OperatingSystem? = null
        private var givenPackageSupplier: ValkeyPackageSupplier? = null
        private var retrievePackage: ThrowableAssert.ThrowingCallable? = null
        private var retrievedPackage: ValkeyPackage? = null

        @BeforeEach
        fun reset() {
            givenOperatingSystem = null
            givenPackageSupplier = null
            retrievePackage = null
            retrievedPackage = null
        }

        @ParameterizedTest(name = "Loading package for {0} from classpath '{1}'")
        @ArgumentsSource(LinuxPackagesOnClasspath::class)
        @DisplayName("Loading Linux package from classpath should work")
        fun `loading Linux package from classpath should work`(
            operatingSystem: OperatingSystem,
            classpathResource: String
        ) {
            givenLinuxPackageSupplier(operatingSystem, classpathResource)
            whenPackageIsRetrieved()
            thenNoErrorOccurs()
            thenLinuxPackageIsValid()
        }

        @ParameterizedTest(name = "Creating Linux package supplier for {0} should not work")
        @EnumSource(
            value = OperatingSystem::class,
            names = ["LINUX_X86_64", "LINUX_ARM64"],
            mode = EnumSource.Mode.EXCLUDE
        )
        @DisplayName("Creating Linux package supplier for unsupported OS should not work")
        fun `creating Linux package supplier for unsupported OS should not work`(operatingSystem: OperatingSystem) {
            givenOperatingSystem(operatingSystem)
            whenLinuxPackageFromClasspathSupplierIsCreated()
            thenValidationErrorOccursSinceOperatingSystemIsNotLinux()
        }

        @ParameterizedTest(name = "Loading package for {0} from classpath '{1}'")
        @ArgumentsSource(MacOsPackagesOnClasspath::class)
        @DisplayName("Loading MacOS package from classpath should work")
        fun `loading MacOS package from classpath should work`(
            operatingSystem: OperatingSystem,
            classpathResource: String
        ) {
            givenMacOsPackageSupplier(operatingSystem, classpathResource)
            whenPackageIsRetrieved()
            thenNoErrorOccurs()
            thenMacOsPackageIsValid()
        }

        @ParameterizedTest(name = "Creating MacOS package supplier for {0} should not work")
        @EnumSource(
            value = OperatingSystem::class,
            names = ["MAC_OS_X86_64", "MAC_OS_ARM64"],
            mode = EnumSource.Mode.EXCLUDE
        )
        @DisplayName("Creating MacOS package supplier for unsupported OS should not work")
        fun `creating MacOS package supplier for unsupported OS should not work`(operatingSystem: OperatingSystem) {
            givenOperatingSystem(operatingSystem)
            whenMacOsPackageFromClasspathSupplierIsCreated()
            thenValidationErrorOccursSinceOperatingSystemIsNotMacOs()
        }

        @Test
        @DisplayName("Loading Windows package from classpath should work")
        fun `loading Windows package from classpath should work`() {
            givenWindowsPackageSupplier()
            whenPackageIsRetrieved()
            thenNoErrorOccurs()
            thenWindowsPackageIsValid()
        }

        private fun givenLinuxPackageSupplier(
            operatingSystem: OperatingSystem,
            classpathResource: String
        ) {
            givenOperatingSystem = operatingSystem
            givenPackageSupplier = loadValkeyIoLinuxPackageFromClasspath(
                classpathResource = classpathResource,
                operatingSystem = operatingSystem
            )
        }

        private fun givenMacOsPackageSupplier(
            operatingSystem: OperatingSystem,
            classpathResource: String
        ) {
            givenOperatingSystem = operatingSystem
            givenPackageSupplier = loadMacPortsPackageFromClasspath(
                classpathResource = classpathResource,
                operatingSystem = operatingSystem
            )
        }

        private fun givenWindowsPackageSupplier() {
            givenOperatingSystem = OperatingSystem.WINDOWS_X86_64
            givenPackageSupplier = loadWinX64MemuraiPackageFromClasspath(
                classpathResource = "/valkey-packages/memuraideveloper.4.1.7.nupkg"
            )
        }

        private fun givenOperatingSystem(operatingSystem: OperatingSystem) {
            givenOperatingSystem = operatingSystem
        }

        private fun whenPackageIsRetrieved() {
            retrievePackage = ThrowableAssert.ThrowingCallable {
                retrievedPackage = givenPackageSupplier!!.retrievePackage()
            }
        }

        private fun whenLinuxPackageFromClasspathSupplierIsCreated() {
            retrievePackage = ThrowableAssert.ThrowingCallable {
                loadValkeyIoLinuxPackageFromClasspath(
                    classpathResource = "/valkey-packages/valkey-9.0.2-jammy-x86_64.tar.gz",
                    operatingSystem = givenOperatingSystem!!
                )
            }
        }

        private fun whenMacOsPackageFromClasspathSupplierIsCreated() {
            retrievePackage = ThrowableAssert.ThrowingCallable {
                loadMacPortsPackageFromClasspath(
                    classpathResource = "/valkey-packages/valkey-9.0.2_0.darwin_24.x86_64.tbz2",
                    operatingSystem = givenOperatingSystem!!
                )
            }
        }

        private fun thenNoErrorOccurs() {
            assertThatCode(retrievePackage!!).doesNotThrowAnyException()
        }

        private fun thenValidationErrorOccursSinceOperatingSystemIsNotLinux() {
            assertThatCode(retrievePackage!!).isExactlyInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Operating system must be either ${OperatingSystem.LINUX_X86_64} or ${OperatingSystem.LINUX_ARM64}")
        }

        private fun thenValidationErrorOccursSinceOperatingSystemIsNotMacOs() {
            assertThatCode(retrievePackage!!).isExactlyInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Operating system must be either ${OperatingSystem.MAC_OS_X86_64} or ${OperatingSystem.MAC_OS_ARM64}")
        }

        private fun thenLinuxPackageIsValid() {
            assertThat(retrievedPackage).isNotNull
            assertThat(retrievedPackage!!.path).exists()
            assertThat(retrievedPackage!!.version).isEqualTo(DEFAULT_VALKEY_LINUX_VERSION)
            assertThat(retrievedPackage!!.operatingSystem).isEqualTo(givenOperatingSystem)
        }

        private fun thenMacOsPackageIsValid() {
            assertThat(retrievedPackage).isNotNull
            assertThat(retrievedPackage!!.path).exists()
            assertThat(retrievedPackage!!.version).isEqualTo(DEFAULT_VALKEY_MAC_OS_VERSION)
            assertThat(retrievedPackage!!.operatingSystem).isEqualTo(givenOperatingSystem)
        }

        private fun thenWindowsPackageIsValid() {
            assertThat(retrievedPackage).isNotNull
            assertThat(retrievedPackage!!.path).exists()
            assertThat(retrievedPackage!!.version).isEqualTo(DEFAULT_MEMURAI_VERSION)
            assertThat(retrievedPackage!!.operatingSystem).isEqualTo(givenOperatingSystem)
        }
    }

    @Nested
    @DisplayName("Tests for download-based package suppliers")
    inner class DownloadPackageSuppliers {

        private var supplierCreation: (() -> ValkeyPackageSupplier)? = null
        private var createSupplier: ThrowableAssert.ThrowingCallable? = null
        private var createdSupplier: ValkeyPackageSupplier? = null
        private var retrievePackage: ThrowableAssert.ThrowingCallable? = null
        private var retrievedPackage: ValkeyPackage? = null
        private var connectProxy: ConnectCountingProxy? = null

        @JvmField
        @RegisterExtension
        val logs: LogCapturer =
            LogCapturer.create().captureForLogger("io.github.tobi.laa.embedded.valkey.valkeypackage", Level.WARN)

        @BeforeEach
        fun reset() {
            supplierCreation = null
            createSupplier = null
            createdSupplier = null
            retrievePackage = null
            retrievedPackage = null
            connectProxy = null
        }

        @AfterEach
        fun cleanUpProxy() {
            connectProxy?.close()
        }

        @Test
        @DisplayName("Should accept a supported Linux operating system with null checksum")
        fun `download Linux package supplier accepts supported OS`() {
            givenLinuxDownloadSupplier()
            whenSupplierIsCreated()
            thenNoErrorOccurs()
            thenSupplierIsValid(OperatingSystem.LINUX_X86_64)
        }

        @Test
        @DisplayName("Creating Linux download supplier with null checksum should log a warning")
        fun `download Linux package supplier with null checksum logs warning`() {
            givenLinuxDownloadSupplier()
            whenSupplierIsCreated()
            thenNoErrorOccurs()
            thenChecksumWarningIsLogged(DEFAULT_VALKEY_LINUX_VERSION, OperatingSystem.LINUX_X86_64)
        }

        @Test
        @DisplayName("Should reject an unsupported Linux operating system")
        fun `download Linux package supplier rejects unsupported OS`() {
            givenLinuxDownloadSupplierWithUnsupportedOs()
            whenSupplierIsCreated()
            thenValidationErrorOccurs()
        }

        @Test
        @DisplayName("Creating macOS download supplier with null checksum should log a warning")
        fun `download macOS package supplier with null checksum logs warning`() {
            givenMacOsDownloadSupplier()
            whenSupplierIsCreated()
            thenNoErrorOccurs()
            thenChecksumWarningIsLogged(DEFAULT_VALKEY_MAC_OS_VERSION, OperatingSystem.MAC_OS_X86_64)
        }

        @Test
        @DisplayName("Should reject an unsupported macOS operating system")
        fun `download macOS package supplier rejects unsupported OS`() {
            givenMacOsDownloadSupplierWithUnsupportedOs()
            whenSupplierIsCreated()
            thenValidationErrorOccursSinceOperatingSystemIsNotMacOs()
        }

        @Test
        @DisplayName("Should require a build file path for macOS packages with unknown version")
        fun `download macOS package supplier requires build file path`() {
            givenMacOsDownloadSupplierWithUnknownVersion()
            whenSupplierIsCreated()
            thenValidationErrorOccurs()
        }

        @Test
        @DisplayName("Creating Windows download supplier with null checksum should succeed and log a warning")
        fun `download windows package supplier allows missing checksum`() {
            givenWindowsDownloadSupplier()
            whenSupplierIsCreated()
            thenNoErrorOccurs()
            thenWindowsSupplierIsValid()
            thenWindowsChecksumWarningIsLogged()
        }

        @Test
        @DisplayName("Downloading a Linux package through a proxy should route the request through the proxy")
        fun `download Linux package through proxy routes request through proxy`() {
            givenLinuxDownloadSupplierWithProxy()
            whenPackageIsRetrievedFromSupplier()
            thenNoRetrievalErrorOccurs()
            thenPackageIsDownloadedSuccessfully()
            thenRequestWasRoutedThroughProxy()
        }

        private fun givenLinuxDownloadSupplier() {
            supplierCreation = {
                downloadLinuxPackageFromValkeyIo(
                    operatingSystem = OperatingSystem.LINUX_X86_64,
                    sha256FileChecksum = null
                )
            }
        }

        private fun givenLinuxDownloadSupplierWithUnsupportedOs() {
            supplierCreation = {
                downloadLinuxPackageFromValkeyIo(operatingSystem = OperatingSystem.MAC_OS_X86_64)
            }
        }

        private fun givenMacOsDownloadSupplier() {
            supplierCreation = {
                downloadMacOsPackageFromMacPorts(sha256FileChecksum = null)
            }
        }

        private fun givenMacOsDownloadSupplierWithUnsupportedOs() {
            supplierCreation = {
                downloadMacOsPackageFromMacPorts(
                    operatingSystem = OperatingSystem.LINUX_X86_64,
                    buildFilePath = "valkey-${DEFAULT_VALKEY_MAC_OS_VERSION}_0.darwin_24.x86_64.tbz2"
                )
            }
        }

        private fun givenMacOsDownloadSupplierWithUnknownVersion() {
            supplierCreation = {
                downloadMacOsPackageFromMacPorts(valkeyVersion = "0.0.0")
            }
        }

        private fun givenWindowsDownloadSupplier() {
            supplierCreation = {
                downloadWinX64MemuraiPackageFromNuget(sha256FileChecksum = null)
            }
        }

        private fun givenLinuxDownloadSupplierWithProxy() {
            connectProxy = ConnectCountingProxy().also { it.start() }
            createdSupplier = downloadLinuxPackageFromValkeyIo(
                proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("localhost", connectProxy!!.port))
            )
            val downloader = createdSupplier as ValkeyPackageDownloader
            Files.deleteIfExists(downloader.cacheFileLocation)
        }

        private fun whenSupplierIsCreated() {
            createSupplier = ThrowableAssert.ThrowingCallable {
                createdSupplier = supplierCreation!!()
            }
        }

        private fun whenPackageIsRetrievedFromSupplier() {
            retrievePackage = ThrowableAssert.ThrowingCallable {
                retrievedPackage = createdSupplier!!.retrievePackage()
            }
        }

        private fun thenNoErrorOccurs() {
            assertThatCode(createSupplier!!).doesNotThrowAnyException()
        }

        private fun thenSupplierIsValid(expectedOperatingSystem: OperatingSystem) {
            assertThat(createdSupplier).isInstanceOf(ValkeyPackageDownloader::class.java)
            val downloader = createdSupplier as ValkeyPackageDownloader
            assertThat(downloader.operatingSystem).isEqualTo(expectedOperatingSystem)
            assertThat(downloader.sha256FileChecksum).isNull()
            assertThat(downloader.verifyFileChecksum).isFalse()
        }

        private fun thenWindowsSupplierIsValid() {
            assertThat(createdSupplier).isInstanceOf(ValkeyPackageDownloader::class.java)
            val downloader = createdSupplier as ValkeyPackageDownloader
            assertThat(downloader.sha256FileChecksum).isNull()
            assertThat(downloader.verifyFileChecksum).isFalse()
        }

        private fun thenValidationErrorOccurs() {
            assertThatCode(createSupplier!!).isExactlyInstanceOf(IllegalArgumentException::class.java)
        }

        private fun thenChecksumWarningIsLogged(
            expectedVersion: String,
            expectedOperatingSystem: OperatingSystem
        ) {
            logs.assertContains("No SHA-256 checksum present for Valkey version $expectedVersion and operating system ${expectedOperatingSystem.displayName}. File integrity will not be verified!")
        }

        private fun thenWindowsChecksumWarningIsLogged() {
            logs.assertContains("No SHA-256 checksum present for Memurai Developer version $DEFAULT_MEMURAI_VERSION. File integrity will not be verified!")
        }

        private fun thenNoRetrievalErrorOccurs() {
            assertThatCode(retrievePackage!!).doesNotThrowAnyException()
        }

        private fun thenPackageIsDownloadedSuccessfully() {
            assertThat(retrievedPackage).isNotNull
            assertThat(retrievedPackage!!.path).exists()
            assertThat(retrievedPackage!!.version).isEqualTo(DEFAULT_VALKEY_LINUX_VERSION)
            assertThat(retrievedPackage!!.operatingSystem).isEqualTo(OperatingSystem.LINUX_X86_64)
        }

        private fun thenRequestWasRoutedThroughProxy() {
            assertThat(connectProxy!!.requestCount).isGreaterThan(0)
        }

        private fun thenValidationErrorOccursSinceOperatingSystemIsNotMacOs() {
            assertThatCode(createSupplier!!).isExactlyInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Operating system must be either ${OperatingSystem.MAC_OS_X86_64} or ${OperatingSystem.MAC_OS_ARM64}")
        }
    }

    class LinuxPackagesOnClasspath : ArgumentsProvider {
        override fun provideArguments(
            parameters: ParameterDeclarations,
            context: ExtensionContext
        ): Stream<Arguments> {
            return Stream.of(
                arguments(OperatingSystem.LINUX_X86_64, "/valkey-packages/valkey-9.0.2-jammy-x86_64.tar.gz"),
                arguments(OperatingSystem.LINUX_ARM64, "/valkey-packages/valkey-9.0.2-jammy-arm64.tar.gz")
            )
        }
    }

    class MacOsPackagesOnClasspath : ArgumentsProvider {
        override fun provideArguments(
            parameters: ParameterDeclarations,
            context: ExtensionContext
        ): Stream<Arguments> {
            return Stream.of(
                arguments(OperatingSystem.MAC_OS_X86_64, "/valkey-packages/valkey-9.0.2_0.darwin_24.x86_64.tbz2"),
                arguments(OperatingSystem.MAC_OS_ARM64, "/valkey-packages/valkey-9.0.2_0.darwin_25.arm64.tbz2")
            )
        }
    }
}

/**
 * Minimal HTTP CONNECT proxy that tunnels HTTPS connections and counts requests.
 * Used to verify that the proxy parameter is actually used during downloads.
 */
private class ConnectCountingProxy : AutoCloseable {
    private val serverSocket = ServerSocket(0)
    val port: Int = serverSocket.localPort
    private val executor = Executors.newCachedThreadPool()
    private val connectRequests = AtomicInteger(0)

    fun start() {
        executor.submit {
            while (!serverSocket.isClosed) {
                try {
                    val client = serverSocket.accept()
                    connectRequests.incrementAndGet()
                    handleConnect(client)
                } catch (_: IOException) {
                }
            }
        }
    }

    val requestCount: Int get() = connectRequests.get()

    private fun handleConnect(client: Socket) {
        executor.submit {
            client.use { clientSocket ->
                val input = clientSocket.getInputStream()
                val requestLine = readLine(input)
                val parts = requestLine.split(" ")
                if (parts.size < 2 || parts[0] != "CONNECT") return@submit
                val hostPort = parts[1].split(":")
                // Drain remaining headers
                while (readLine(input).isNotEmpty()) {
                }
                Socket(hostPort[0], hostPort[1].toInt()).use { target ->
                    clientSocket.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                    clientSocket.getOutputStream().flush()
                    val toTarget = executor.submit {
                        try {
                            input.transferTo(target.getOutputStream())
                        } catch (_: IOException) {
                        }
                    }
                    try {
                        target.getInputStream().transferTo(clientSocket.getOutputStream())
                    } catch (_: IOException) {
                    }
                    toTarget.cancel(true)
                }
            }
        }
    }

    /** Reads a single line from the input stream without buffering ahead. */
    private fun readLine(input: InputStream): String {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1 || b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
        }
        return sb.toString()
    }

    override fun close() {
        serverSocket.close()
        executor.shutdownNow()
    }
}
