package io.github.tobi.laa.embedded.valkey;

import io.github.tobi.laa.embedded.valkey.cluster.sharded.Shard;
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder;
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallation;
import io.github.tobi.laa.embedded.valkey.installation.ValkeyPackageExtractor;
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem;
import io.github.tobi.laa.embedded.valkey.ports.PortProvider;
import io.github.tobi.laa.embedded.valkey.process.ValkeyProcess;
import io.github.tobi.laa.embedded.valkey.testing.ScriptBehavior;
import io.github.tobi.laa.embedded.valkey.valkeypackage.ArchiveType;
import io.github.tobi.laa.embedded.valkey.valkeypackage.ClasspathPackageSupplier;
import io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackage;
import io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackageDownloader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt.downloadAndInstallLinuxPackageFromValkeyIo;
import static io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt.downloadAndInstallMacOsPackageFromMacports;
import static io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt.downloadAndInstallMemuraiDeveloperForX64FromNuget;
import static io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt.installMacPortsPackageFromClasspath;
import static io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt.installValkeyIoLinuxPackageFromClasspath;
import static io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt.installWinX64MemuraiPackageFromClasspath;
import static io.github.tobi.laa.embedded.valkey.testing.TestValkeyInstallationsKt.createExecutableScript;
import static io.github.tobi.laa.embedded.valkey.testing.TestValkeyInstallationsKt.createValkeyInstallation;
import static io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackageSuppliersKt.downloadLinuxPackageFromValkeyIo;
import static io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackageSuppliersKt.downloadMacOsPackageFromMacPorts;
import static io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackageSuppliersKt.downloadWinX64MemuraiPackageFromNuget;
import static io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackageSuppliersKt.loadMacPortsPackageFromClasspath;
import static io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackageSuppliersKt.loadValkeyIoLinuxPackageFromClasspath;
import static io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackageSuppliersKt.loadWinX64MemuraiPackageFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests that exercise Kotlin @JvmOverloads-generated Java overloads and interface default methods.
 * <p>
 * These overloaded methods are only reachable from Java callers, as the Kotlin compiler
 * generates direct calls to the full method with all parameters specified.
 */
@DisplayName("Tests for Java interop coverage (@JvmOverloads and interface defaults)")
class JavaInteropCoverageTest {

    @Nested
    @DisplayName("Valkey interface default method bodies")
    class ValkeyInterfaceDefaults {

        @Test
        @DisplayName("start() with no arguments should delegate to start(awaitReadiness=true, maxWaitTimeSeconds=10)")
        void startNoArgs() throws IOException {
            var valkey = mock(Valkey.class);
            doCallRealMethod().when(valkey).start();
            valkey.start();
            verify(valkey).start(true, 10L);
        }

        @Test
        @DisplayName("stop() with no arguments should delegate to stop(forcibly=false, maxWaitTimeSeconds=10, removeWorkingDir=false)")
        void stopNoArgs() throws IOException {
            var valkey = mock(Valkey.class);
            doCallRealMethod().when(valkey).stop();
            valkey.stop();
            verify(valkey).stop(false, 10L, false);
        }
    }

    @Nested
    @DisplayName("ValkeyInstallationSuppliersKt @JvmOverloads overloads")
    class InstallationSuppliersOverloads {

        @Test
        @DisplayName("downloadAndInstallLinuxPackageFromValkeyIo should accept 0 to 4 optional args (version, OS, installationPath, valkeyVersion)")
        void downloadAndInstallLinuxWithDefaultParams() {
            assertThat(downloadAndInstallLinuxPackageFromValkeyIo()).isNotNull();
            assertThat(downloadAndInstallLinuxPackageFromValkeyIo(null)).isNotNull();
            assertThat(downloadAndInstallLinuxPackageFromValkeyIo(null, OperatingSystem.LINUX_ARM64)).isNotNull();
            assertThat(downloadAndInstallLinuxPackageFromValkeyIo(null, OperatingSystem.LINUX_X86_64, null)).isNotNull();
            assertThat(downloadAndInstallLinuxPackageFromValkeyIo(null, OperatingSystem.LINUX_X86_64, null, "9.0.3")).isNotNull();
        }

        @Test
        @DisplayName("installValkeyIoLinuxPackageFromClasspath should accept classpath with optional OS and installationPath")
        void installLinuxFromClasspathWithDefaultParams() {
            var classpathResource = "/valkey-packages/valkey-9.0.3-jammy-x86_64.tar.gz";
            assertThat(installValkeyIoLinuxPackageFromClasspath(classpathResource)).isNotNull();
            assertThat(installValkeyIoLinuxPackageFromClasspath(classpathResource, OperatingSystem.LINUX_ARM64)).isNotNull();
            assertThat(installValkeyIoLinuxPackageFromClasspath(classpathResource, OperatingSystem.LINUX_X86_64, null)).isNotNull();
        }

        @Test
        @DisplayName("downloadAndInstallMacOsPackageFromMacports should accept 0 to 4 optional args (version, OS, installationPath, valkeyVersion)")
        void downloadAndInstallMacOsWithDefaultParams() {
            assertThat(downloadAndInstallMacOsPackageFromMacports()).isNotNull();
            assertThat(downloadAndInstallMacOsPackageFromMacports(null)).isNotNull();
            assertThat(downloadAndInstallMacOsPackageFromMacports(null, OperatingSystem.MAC_OS_ARM64)).isNotNull();
            assertThat(downloadAndInstallMacOsPackageFromMacports(null, OperatingSystem.MAC_OS_X86_64, null)).isNotNull();
            assertThat(downloadAndInstallMacOsPackageFromMacports(null, OperatingSystem.MAC_OS_X86_64, null, "9.0.3")).isNotNull();
        }

        @Test
        @DisplayName("installMacPortsPackageFromClasspath should accept classpath with optional OS and installationPath")
        void installMacOsFromClasspathWithDefaultParams() {
            var classpathResource = "/valkey-packages/valkey-9.0.3_0.darwin_24.x86_64.tbz2";
            assertThat(installMacPortsPackageFromClasspath(classpathResource)).isNotNull();
            assertThat(installMacPortsPackageFromClasspath(classpathResource, OperatingSystem.MAC_OS_ARM64)).isNotNull();
            assertThat(installMacPortsPackageFromClasspath(classpathResource, OperatingSystem.MAC_OS_X86_64, null)).isNotNull();
        }

        @Test
        @DisplayName("downloadAndInstallMemuraiDeveloperForX64FromNuget should accept 0 to 3 optional args (version, installationPath, proxy)")
        void downloadAndInstallMemuraiWithDefaultParams() {
            assertThat(downloadAndInstallMemuraiDeveloperForX64FromNuget()).isNotNull();
            assertThat(downloadAndInstallMemuraiDeveloperForX64FromNuget("4.1.7")).isNotNull();
            assertThat(downloadAndInstallMemuraiDeveloperForX64FromNuget("4.1.7", null)).isNotNull();
            assertThat(downloadAndInstallMemuraiDeveloperForX64FromNuget("4.1.7", null, null)).isNotNull();
        }

        @Test
        @DisplayName("installWinX64MemuraiPackageFromClasspath should accept classpath with optional installationPath")
        void installMemuraiFromClasspathWithDefaultParams() {
            var classpathResource = "/valkey-packages/memuraideveloper.4.1.7.nupkg";
            assertThat(installWinX64MemuraiPackageFromClasspath(classpathResource)).isNotNull();
            assertThat(installWinX64MemuraiPackageFromClasspath(classpathResource, null)).isNotNull();
        }
    }

    @Nested
    @DisplayName("ValkeyPackageSuppliersKt @JvmOverloads overloads")
    class PackageSuppliersOverloads {

        @Test
        @DisplayName("downloadLinuxPackageFromValkeyIo should accept 0 to 3 optional args (proxy, OS, version)")
        void downloadLinuxPackageWithDefaultParams() {
            assertThat(downloadLinuxPackageFromValkeyIo()).isNotNull();
            assertThat(downloadLinuxPackageFromValkeyIo(null)).isNotNull();
            assertThat(downloadLinuxPackageFromValkeyIo(null, OperatingSystem.LINUX_ARM64)).isNotNull();
            assertThat(downloadLinuxPackageFromValkeyIo(null, OperatingSystem.LINUX_X86_64, "9.0.3")).isNotNull();
        }

        @Test
        @DisplayName("loadValkeyIoLinuxPackageFromClasspath should accept classpath with optional OS")
        void loadLinuxPackageFromClasspathWithDefaultParams() {
            var classpathResource = "/valkey-packages/valkey-9.0.3-jammy-x86_64.tar.gz";
            assertThat(loadValkeyIoLinuxPackageFromClasspath(classpathResource)).isNotNull();
            assertThat(loadValkeyIoLinuxPackageFromClasspath(classpathResource, OperatingSystem.LINUX_ARM64)).isNotNull();
        }

        @Test
        @DisplayName("downloadMacOsPackageFromMacPorts should accept 0 to 3 optional args (proxy, OS, version)")
        void downloadMacOsPackageWithDefaultParams() {
            assertThat(downloadMacOsPackageFromMacPorts()).isNotNull();
            assertThat(downloadMacOsPackageFromMacPorts(null)).isNotNull();
            assertThat(downloadMacOsPackageFromMacPorts(null, OperatingSystem.MAC_OS_ARM64)).isNotNull();
            assertThat(downloadMacOsPackageFromMacPorts(null, OperatingSystem.MAC_OS_X86_64, "9.0.3")).isNotNull();
        }

        @Test
        @DisplayName("loadMacPortsPackageFromClasspath should accept classpath with optional OS")
        void loadMacOsPackageFromClasspathWithDefaultParams() {
            var classpathResource = "/valkey-packages/valkey-9.0.3_0.darwin_24.x86_64.tbz2";
            assertThat(loadMacPortsPackageFromClasspath(classpathResource)).isNotNull();
            assertThat(loadMacPortsPackageFromClasspath(classpathResource, OperatingSystem.MAC_OS_ARM64)).isNotNull();
        }

        @Test
        @DisplayName("downloadWinX64MemuraiPackageFromNuget should accept 0 to 2 optional args (proxy, version)")
        void downloadMemuraiPackageWithDefaultParams() {
            assertThat(downloadWinX64MemuraiPackageFromNuget()).isNotNull();
            assertThat(downloadWinX64MemuraiPackageFromNuget(null)).isNotNull();
            assertThat(downloadWinX64MemuraiPackageFromNuget(null, "4.1.7")).isNotNull();
        }

        @Test
        @DisplayName("loadWinX64MemuraiPackageFromClasspath should return a supplier for the given classpath")
        void loadMemuraiPackageFromClasspathWithDefaultParams() {
            assertThat(loadWinX64MemuraiPackageFromClasspath("/valkey-packages/memuraideveloper.4.1.7.nupkg")).isNotNull();
        }
    }

    @Nested
    @DisplayName("ValkeyPackageDownloader @JvmOverloads constructor overloads")
    class PackageDownloaderOverloads {

        @Test
        @DisplayName("Constructor should accept 5 to 11 args, defaulting proxy, cacheDownload, cacheFileLocation, sha256, verifyChecksum, downloadLocation")
        void constructorWithVaryingOptionalParams() {
            var binaryPath = Paths.get("bin", "valkey-server");
            var downloadUri = URI.create("https://example.com/valkey.tar.gz");
            var cacheLocation = Paths.get("/tmp/cache.tar.gz");
            var downloadLocation = Paths.get("/tmp/download.tar.gz");
            // 5-arg (required only)
            assertThat(new ValkeyPackageDownloader("9.0.3", OperatingSystem.LINUX_X86_64, binaryPath, ArchiveType.TAR_GZ, downloadUri)).isNotNull();
            // 6-arg (+ proxy)
            assertThat(new ValkeyPackageDownloader("9.0.3", OperatingSystem.LINUX_X86_64, binaryPath, ArchiveType.TAR_GZ, downloadUri, null)).isNotNull();
            // 7-arg (+ proxy + cacheDownload)
            assertThat(new ValkeyPackageDownloader("9.0.3", OperatingSystem.LINUX_X86_64, binaryPath, ArchiveType.TAR_GZ, downloadUri, null, false)).isNotNull();
            // 8-arg (+ proxy + cacheDownload + cacheFileLocation)
            assertThat(new ValkeyPackageDownloader("9.0.3", OperatingSystem.LINUX_X86_64, binaryPath, ArchiveType.TAR_GZ, downloadUri, null, false, cacheLocation)).isNotNull();
            // 9-arg (+ sha256)
            assertThat(new ValkeyPackageDownloader("9.0.3", OperatingSystem.LINUX_X86_64, binaryPath, ArchiveType.TAR_GZ, downloadUri, null, false, cacheLocation, null)).isNotNull();
            // 10-arg (+ verifyChecksum)
            assertThat(new ValkeyPackageDownloader("9.0.3", OperatingSystem.LINUX_X86_64, binaryPath, ArchiveType.TAR_GZ, downloadUri, null, false, cacheLocation, null, false)).isNotNull();
            // 11-arg (+ downloadLocation)
            assertThat(new ValkeyPackageDownloader("9.0.3", OperatingSystem.LINUX_X86_64, binaryPath, ArchiveType.TAR_GZ, downloadUri, null, false, cacheLocation, null, false, downloadLocation)).isNotNull();
        }
    }

    @Nested
    @DisplayName("ValkeyPackageExtractor @JvmOverloads constructor overloads")
    class PackageExtractorOverloads {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Constructor should accept 1 to 3 args, defaulting installationPath and alwaysExtract")
        void constructorWithVaryingOptionalParams() throws IOException {
            var packagePath = tempDir.resolve("dummy.tar.gz");
            Files.createFile(packagePath);
            var valkeyPackage = new ValkeyPackage(
                    "9.0.3", OperatingSystem.LINUX_X86_64, packagePath, Paths.get("bin", "valkey-server"), ArchiveType.TAR_GZ
            );
            // 1-arg (package only, defaults installationPath and alwaysExtract)
            assertThat(new ValkeyPackageExtractor(valkeyPackage)).isNotNull();
            // 2-arg (package + installationPath, defaults alwaysExtract)
            assertThat(new ValkeyPackageExtractor(valkeyPackage, tempDir.resolve("install1"))).isNotNull();
            // 3-arg (package + installationPath + alwaysExtract)
            assertThat(new ValkeyPackageExtractor(valkeyPackage, tempDir.resolve("install2"), true)).isNotNull();
        }
    }

    @Nested
    @DisplayName("ValkeyConfBuilder @JvmOverloads overloads")
    class ConfBuilderOverloads {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("importConf(Path) without charset should parse config using UTF-8 by default")
        void importConfWithoutCharsetDefaultsToUtf8() throws IOException {
            var confFile = tempDir.resolve("test.conf");
            Files.writeString(confFile, "port 6379" + System.lineSeparator());
            var conf = new ValkeyConfBuilder().importConf(confFile).build();
            assertThat(conf.port()).isEqualTo(6379);
        }
    }

    @Nested
    @DisplayName("ValkeyProcess @JvmOverloads overloads")
    class ProcessOverloads {

        @Test
        @DisplayName("Constructor should accept 1-2 args (valkeyInstallation, optional workingDirectory) with defaults for remaining params")
        void constructorWithVaryingOptionalParams() throws IOException {
            var scriptPath = createExecutableScript(ScriptBehavior.ECHO_READY_AND_SLEEP);
            var installation = createValkeyInstallation(scriptPath);
            // 1-arg constructor (only valkeyInstallation, all defaults)
            var processWithDefaults = new ValkeyProcess(installation);
            assertThat(processWithDefaults.getValkeyInstallation()).isEqualTo(installation);
            // 2-arg constructor (+ workingDirectory)
            var workingDir = Files.createTempDirectory(installation.getInstallationPath(), "wd");
            var processWithWorkDir = new ValkeyProcess(installation, workingDir);
            assertThat(processWithWorkDir.getWorkingDirectory()).isEqualTo(workingDir);
        }

        @Test
        @DisplayName("start/stop should accept 0-1 args, defaulting awaitServerReady and forcibly")
        void startStopWithVaryingOptionalParams() throws IOException {
            var scriptPath = createExecutableScript(ScriptBehavior.ECHO_READY_AND_SLEEP);
            var installation = createValkeyInstallation(scriptPath);
            var process = new ValkeyProcess(installation);
            // start() 0-arg overload (defaults: awaitServerReady=true, maxWaitTimeSeconds=10)
            process.start();
            assertThat(process.getActive()).isTrue();
            // stop() 0-arg overload (defaults: forcibly=false, maxWaitTimeSeconds=10, removeWorkingDirectory=false)
            process.stop();
            assertThat(process.getActive()).isFalse();
        }

        @Test
        @DisplayName("start(boolean)/stop(boolean) should accept a single explicit arg with defaults for remaining")
        void startStopWithOneArg() throws IOException {
            var scriptPath = createExecutableScript(ScriptBehavior.ECHO_READY_AND_SLEEP);
            var installation = createValkeyInstallation(scriptPath);
            var process = new ValkeyProcess(installation);
            // start(boolean) 1-arg overload (awaitServerReady=false, default maxWaitTimeSeconds)
            process.start(false);
            assertThat(process.getActive()).isTrue();
            // stop(boolean) 1-arg overload (forcibly=true, default maxWaitTimeSeconds)
            process.stop(true);
            assertThat(process.getActive()).isFalse();
        }

        @Test
        @DisplayName("start(boolean, long)/stop(boolean, long) should accept two explicit args")
        void startStopWithTwoArgs() throws IOException {
            var scriptPath = createExecutableScript(ScriptBehavior.ECHO_READY_AND_SLEEP);
            var installation = createValkeyInstallation(scriptPath);
            var process = new ValkeyProcess(installation);
            // start(boolean, long) - awaitServerReady=true, maxWaitTimeSeconds=3
            process.start(true, 3L);
            assertThat(process.getActive()).isTrue();
            // stop(boolean, long) 2-arg overload - forcibly=true, maxWaitTimeSeconds=1
            process.stop(true, 1L);
            assertThat(process.getActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("ValkeyInstallation @JvmOverloads constructor overloads")
    class InstallationOverloads {

        @Test
        @DisplayName("4-arg constructor without distributionType should default to VALKEY")
        void constructorWithoutDistributionTypeDefaultsToValkey() throws IOException {
            var installDir = Files.createTempDirectory("valkey-install");
            var binary = Files.createTempFile(installDir, "valkey-server", ".sh");
            binary.toFile().setExecutable(true);
            var installation = new ValkeyInstallation(
                    "9.0.3", OperatingSystem.LINUX_X86_64, installDir, binary
            );
            assertThat(installation.getDistributionType()).isEqualTo(io.github.tobi.laa.embedded.valkey.installation.DistributionType.VALKEY);
        }
    }

    @Nested
    @DisplayName("ClasspathPackageSupplier @JvmOverloads constructor overloads")
    class ClasspathSupplierOverloads {

        @Test
        @DisplayName("5-arg constructor without distributionType should default to VALKEY")
        void constructorWithoutDistributionTypeDefaultsToValkey() {
            var supplier = new ClasspathPackageSupplier(
                    "/nonexistent/pkg.tar.gz", "9.0.3", OperatingSystem.LINUX_X86_64,
                    Paths.get("bin", "valkey-server"), ArchiveType.TAR_GZ
            );
            assertThat(supplier).isNotNull();
        }
    }

    @Nested
    @DisplayName("Shard PortProvider constructor overloads")
    class ShardOverloads {

        @Test
        @DisplayName("Shard(name, PortProvider, replicaCount) should allocate the requested number of replica ports")
        void shardWithPortProviderAllocatesReplicaPorts() {
            var shard = new Shard("test-shard", new PortProvider(), 2);
            assertThat(shard.getReplicaPorts()).hasSize(2);
            assertThat(shard.getName()).isEqualTo("test-shard");
        }
    }
}
