package io.github.tobi.laa.embedded.valkey.valkeypackage

import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.support.ParameterDeclarations
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
                classpathResource = "/valkey-packages/memuraideveloper.4.1.6.nupkg"
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
                    classpathResource = "/valkey-packages/valkey-8.1.3-jammy-x86_64.tar.gz",
                    operatingSystem = givenOperatingSystem!!
                )
            }
        }

        private fun whenMacOsPackageFromClasspathSupplierIsCreated() {
            retrievePackage = ThrowableAssert.ThrowingCallable {
                loadMacPortsPackageFromClasspath(
                    classpathResource = "/valkey-packages/valkey-8.1.3_0.darwin_24.x86_64.tbz2",
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

    class LinuxPackagesOnClasspath : ArgumentsProvider {
        override fun provideArguments(
            parameters: ParameterDeclarations,
            context: ExtensionContext
        ): Stream<Arguments> {
            return Stream.of(
                arguments(OperatingSystem.LINUX_X86_64, "/valkey-packages/valkey-8.1.3-jammy-x86_64.tar.gz"),
                arguments(OperatingSystem.LINUX_ARM64, "/valkey-packages/valkey-8.1.3-jammy-arm64.tar.gz")
            )
        }
    }

    class MacOsPackagesOnClasspath : ArgumentsProvider {
        override fun provideArguments(
            parameters: ParameterDeclarations,
            context: ExtensionContext
        ): Stream<Arguments> {
            return Stream.of(
                arguments(OperatingSystem.MAC_OS_X86_64, "/valkey-packages/valkey-8.1.3_0.darwin_24.x86_64.tbz2"),
                arguments(OperatingSystem.MAC_OS_ARM64, "/valkey-packages/valkey-8.1.3_0.darwin_25.arm64.tbz2")
            )
        }
    }
}