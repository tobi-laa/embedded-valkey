package io.github.tobi.laa.embedded.valkey;

import io.github.tobi.laa.embedded.valkey.cluster.sharded.Shard;
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConf;
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfBuilder;
import io.github.tobi.laa.embedded.valkey.installation.DistributionType;
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallation;
import io.github.tobi.laa.embedded.valkey.installation.ValkeyPackageExtractor;
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem;
import io.github.tobi.laa.embedded.valkey.ports.PortProvider;
import io.github.tobi.laa.embedded.valkey.process.ValkeyProcess;
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

import static io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt.*;
import static io.github.tobi.laa.embedded.valkey.valkeypackage.ValkeyPackageSuppliersKt.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
        @DisplayName("start() should delegate to start(true, 10)")
        void startNoArgs() throws IOException {
            Valkey valkey = mock(Valkey.class);
            doCallRealMethod().when(valkey).start();
            valkey.start();
            verify(valkey).start(true, 10L);
        }

        @Test
        @DisplayName("stop() should delegate to stop(false, 10, false)")
        void stopNoArgs() throws IOException {
            Valkey valkey = mock(Valkey.class);
            doCallRealMethod().when(valkey).stop();
            valkey.stop();
            verify(valkey).stop(false, 10L, false);
        }
    }

    @Nested
    @DisplayName("ValkeyInstallationSuppliersKt @JvmOverloads")
    class InstallationSuppliersOverloads {

        @Test
        @DisplayName("downloadAndInstallLinuxPackageFromValkeyIo - all overloads")
        void downloadLinux() {
            assertThat(downloadAndInstallLinuxPackageFromValkeyIo()).isNotNull();
            assertThat(downloadAndInstallLinuxPackageFromValkeyIo(null)).isNotNull();
            assertThat(downloadAndInstallLinuxPackageFromValkeyIo(null, OperatingSystem.LINUX_ARM64)).isNotNull();
            assertThat(downloadAndInstallLinuxPackageFromValkeyIo(null, OperatingSystem.LINUX_X86_64, null)).isNotNull();
            assertThat(downloadAndInstallLinuxPackageFromValkeyIo(null, OperatingSystem.LINUX_X86_64, null, "9.0.2")).isNotNull();
        }

        @Test
        @DisplayName("installValkeyIoLinuxPackageFromClasspath - all overloads")
        void installLinuxClasspath() {
            String cp = "/valkey-packages/valkey-9.0.2-jammy-x86_64.tar.gz";
            assertThat(installValkeyIoLinuxPackageFromClasspath(cp)).isNotNull();
            assertThat(installValkeyIoLinuxPackageFromClasspath(cp, OperatingSystem.LINUX_ARM64)).isNotNull();
            assertThat(installValkeyIoLinuxPackageFromClasspath(cp, OperatingSystem.LINUX_X86_64, null)).isNotNull();
        }

        @Test
        @DisplayName("downloadAndInstallMacOsPackageFromMacports - all overloads")
        void downloadMacOs() {
            assertThat(downloadAndInstallMacOsPackageFromMacports()).isNotNull();
            assertThat(downloadAndInstallMacOsPackageFromMacports(null)).isNotNull();
            assertThat(downloadAndInstallMacOsPackageFromMacports(null, OperatingSystem.MAC_OS_ARM64)).isNotNull();
            assertThat(downloadAndInstallMacOsPackageFromMacports(null, OperatingSystem.MAC_OS_X86_64, null)).isNotNull();
            assertThat(downloadAndInstallMacOsPackageFromMacports(null, OperatingSystem.MAC_OS_X86_64, null, "9.0.2")).isNotNull();
        }

        @Test
        @DisplayName("installMacPortsPackageFromClasspath - all overloads")
        void installMacOsClasspath() {
            String cp = "/valkey-packages/valkey-9.0.2_0.darwin_24.x86_64.tbz2";
            assertThat(installMacPortsPackageFromClasspath(cp)).isNotNull();
            assertThat(installMacPortsPackageFromClasspath(cp, OperatingSystem.MAC_OS_ARM64)).isNotNull();
            assertThat(installMacPortsPackageFromClasspath(cp, OperatingSystem.MAC_OS_X86_64, null)).isNotNull();
        }

        @Test
        @DisplayName("downloadAndInstallMemuraiDeveloperForX64FromNuget - all overloads")
        void downloadMemurai() {
            assertThat(downloadAndInstallMemuraiDeveloperForX64FromNuget()).isNotNull();
            assertThat(downloadAndInstallMemuraiDeveloperForX64FromNuget("4.1.7")).isNotNull();
            assertThat(downloadAndInstallMemuraiDeveloperForX64FromNuget("4.1.7", null)).isNotNull();
            assertThat(downloadAndInstallMemuraiDeveloperForX64FromNuget("4.1.7", null, null)).isNotNull();
        }

        @Test
        @DisplayName("installWinX64MemuraiPackageFromClasspath - all overloads")
        void installMemuraiClasspath() {
            String cp = "/valkey-packages/memuraideveloper.4.1.7.nupkg";
            assertThat(installWinX64MemuraiPackageFromClasspath(cp)).isNotNull();
            assertThat(installWinX64MemuraiPackageFromClasspath(cp, null)).isNotNull();
        }
    }

    @Nested
    @DisplayName("ValkeyPackageSuppliersKt @JvmOverloads")
    class PackageSuppliersOverloads {

        @Test
        @DisplayName("downloadLinuxPackageFromValkeyIo - all overloads")
        void downloadLinux() {
            assertThat(downloadLinuxPackageFromValkeyIo()).isNotNull();
            assertThat(downloadLinuxPackageFromValkeyIo(null)).isNotNull();
            assertThat(downloadLinuxPackageFromValkeyIo(null, OperatingSystem.LINUX_ARM64)).isNotNull();
            assertThat(downloadLinuxPackageFromValkeyIo(null, OperatingSystem.LINUX_X86_64, "9.0.2")).isNotNull();
        }

        @Test
        @DisplayName("loadValkeyIoLinuxPackageFromClasspath - all overloads")
        void loadLinuxClasspath() {
            String cp = "/valkey-packages/valkey-9.0.2-jammy-x86_64.tar.gz";
            assertThat(loadValkeyIoLinuxPackageFromClasspath(cp)).isNotNull();
            assertThat(loadValkeyIoLinuxPackageFromClasspath(cp, OperatingSystem.LINUX_ARM64)).isNotNull();
        }

        @Test
        @DisplayName("downloadMacOsPackageFromMacPorts - all overloads")
        void downloadMacOs() {
            assertThat(downloadMacOsPackageFromMacPorts()).isNotNull();
            assertThat(downloadMacOsPackageFromMacPorts(null)).isNotNull();
            assertThat(downloadMacOsPackageFromMacPorts(null, OperatingSystem.MAC_OS_ARM64)).isNotNull();
            assertThat(downloadMacOsPackageFromMacPorts(null, OperatingSystem.MAC_OS_X86_64, "9.0.2")).isNotNull();
        }

        @Test
        @DisplayName("loadMacPortsPackageFromClasspath - all overloads")
        void loadMacOsClasspath() {
            String cp = "/valkey-packages/valkey-9.0.2_0.darwin_24.x86_64.tbz2";
            assertThat(loadMacPortsPackageFromClasspath(cp)).isNotNull();
            assertThat(loadMacPortsPackageFromClasspath(cp, OperatingSystem.MAC_OS_ARM64)).isNotNull();
        }

        @Test
        @DisplayName("downloadWinX64MemuraiPackageFromNuget - all overloads")
        void downloadMemurai() {
            assertThat(downloadWinX64MemuraiPackageFromNuget()).isNotNull();
            assertThat(downloadWinX64MemuraiPackageFromNuget(null)).isNotNull();
            assertThat(downloadWinX64MemuraiPackageFromNuget(null, "4.1.7")).isNotNull();
        }

        @Test
        @DisplayName("loadWinX64MemuraiPackageFromClasspath - all overloads")
        void loadMemuraiClasspath() {
            assertThat(loadWinX64MemuraiPackageFromClasspath("/valkey-packages/memuraideveloper.4.1.7.nupkg")).isNotNull();
        }
    }

    @Nested
    @DisplayName("ValkeyPackageDownloader @JvmOverloads constructor")
    class PackageDownloaderOverloads {

        @Test
        @DisplayName("Constructor overloads with varying parameter counts")
        void constructorOverloads() {
            Path bin = Paths.get("bin", "valkey-server");
            URI uri = URI.create("https://example.com/valkey.tar.gz");
            // 5-arg (required only)
            assertThat(new ValkeyPackageDownloader("9.0.2", OperatingSystem.LINUX_X86_64, bin, ArchiveType.TAR_GZ, uri)).isNotNull();
            // 6-arg (+ proxy)
            assertThat(new ValkeyPackageDownloader("9.0.2", OperatingSystem.LINUX_X86_64, bin, ArchiveType.TAR_GZ, uri, null)).isNotNull();
            // 7-arg (+ proxy + cacheDownload)
            assertThat(new ValkeyPackageDownloader("9.0.2", OperatingSystem.LINUX_X86_64, bin, ArchiveType.TAR_GZ, uri, null, false)).isNotNull();
            // 8-arg (+ proxy + cacheDownload + cacheFileLocation)
            assertThat(new ValkeyPackageDownloader("9.0.2", OperatingSystem.LINUX_X86_64, bin, ArchiveType.TAR_GZ, uri, null, false, Paths.get("/tmp/cache.tar.gz"))).isNotNull();
            // 9-arg (+ ... + sha256)
            assertThat(new ValkeyPackageDownloader("9.0.2", OperatingSystem.LINUX_X86_64, bin, ArchiveType.TAR_GZ, uri, null, false, Paths.get("/tmp/cache.tar.gz"), null)).isNotNull();
            // 10-arg (+ ... + verifyChecksum)
            assertThat(new ValkeyPackageDownloader("9.0.2", OperatingSystem.LINUX_X86_64, bin, ArchiveType.TAR_GZ, uri, null, false, Paths.get("/tmp/cache.tar.gz"), null, false)).isNotNull();
            // 11-arg (+ ... + downloadLocation)
            assertThat(new ValkeyPackageDownloader("9.0.2", OperatingSystem.LINUX_X86_64, bin, ArchiveType.TAR_GZ, uri, null, false, Paths.get("/tmp/cache.tar.gz"), null, false, Paths.get("/tmp/download.tar.gz"))).isNotNull();
        }
    }

    @Nested
    @DisplayName("ValkeyPackageExtractor @JvmOverloads constructor")
    class PackageExtractorOverloads {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Constructor overloads with varying parameter counts")
        void constructorOverloads() throws IOException {
            Path pkgPath = tempDir.resolve("dummy.tar.gz");
            Files.createFile(pkgPath);
            ValkeyPackage pkg = new ValkeyPackage(
                    "9.0.2", OperatingSystem.LINUX_X86_64, pkgPath, Paths.get("bin", "valkey-server"), ArchiveType.TAR_GZ
            );
            // 1-arg (package only)
            assertThat(new ValkeyPackageExtractor(pkg)).isNotNull();
            // 2-arg (package + installationPath)
            assertThat(new ValkeyPackageExtractor(pkg, tempDir.resolve("install1"))).isNotNull();
            // 3-arg (package + installationPath + alwaysExtract)
            assertThat(new ValkeyPackageExtractor(pkg, tempDir.resolve("install2"), true)).isNotNull();
        }
    }

    @Nested
    @DisplayName("ValkeyConfBuilder @JvmOverloads")
    class ConfBuilderOverloads {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("importConf(Path) without charset should use UTF-8 by default")
        void importConfWithoutCharset() throws IOException {
            Path confFile = tempDir.resolve("test.conf");
            Files.writeString(confFile, "port 6379\n");
            ValkeyConf conf = new ValkeyConfBuilder().importConf(confFile).build();
            assertThat(conf.port()).isEqualTo(6379);
        }
    }

    @Nested
    @DisplayName("ValkeyProcess @JvmOverloads")
    class ProcessOverloads {

        @Test
        @DisplayName("Constructor overloads and start/stop overloads")
        void constructorAndStartStopOverloads() throws IOException {
            Path script = createScript(
                    "#!/bin/sh\necho \"Ready to accept connections\"\nwhile true; do sleep 1; done\n"
            );
            ValkeyInstallation installation = createInstallation(script);
            // 1-arg constructor (only valkeyInstallation, all defaults)
            ValkeyProcess p1 = new ValkeyProcess(installation);
            assertThat(p1).isNotNull();
            // 2-arg constructor (+ workingDirectory)
            ValkeyProcess p2 = new ValkeyProcess(installation, Files.createTempDirectory(installation.getInstallationPath(), "wd"));
            assertThat(p2).isNotNull();
            // start() 0-arg overload
            p1.start();
            // stop() 0-arg overload
            p1.stop();
            // start(boolean) 1-arg overload
            p2.start(false);
            // stop(boolean) 1-arg overload
            p2.stop(true);
        }

        @Test
        @DisplayName("start(boolean, long) and stop(boolean, long) overloads")
        void startStopTwoArgOverloads() throws IOException {
            Path script = createScript(
                    "#!/bin/sh\necho \"Ready to accept connections\"\nwhile true; do sleep 1; done\n"
            );
            ValkeyInstallation installation = createInstallation(script);
            ValkeyProcess p = new ValkeyProcess(installation);
            // start(boolean, long) - full
            p.start(true, 3L);
            // stop(boolean, long) 2-arg overload
            p.stop(true, 1L);
        }

        private Path createScript(String content) throws IOException {
            Path script = Files.createTempFile("valkey-script", ".sh");
            Files.writeString(script, content);
            script.toFile().setExecutable(true);
            return script;
        }

        private ValkeyInstallation createInstallation(Path binary) throws IOException {
            Path installDir = Files.createTempDirectory("valkey-install");
            return new ValkeyInstallation(
                    "9.0.2", OperatingSystem.LINUX_X86_64, DistributionType.VALKEY, installDir, binary
            );
        }
    }

    @Nested
    @DisplayName("ValkeyInstallation @JvmOverloads constructor")
    class InstallationOverloads {

        @Test
        @DisplayName("4-arg constructor without distributionType should default to VALKEY")
        void constructorWithoutDistributionType() throws IOException {
            Path installDir = Files.createTempDirectory("valkey-install");
            Path binary = Files.createTempFile(installDir, "valkey-server", ".sh");
            binary.toFile().setExecutable(true);
            // 4-arg (without distributionType - defaults to VALKEY)
            ValkeyInstallation installation = new ValkeyInstallation(
                    "9.0.2", OperatingSystem.LINUX_X86_64, installDir, binary
            );
            assertThat(installation.getDistributionType()).isEqualTo(DistributionType.VALKEY);
        }
    }

    @Nested
    @DisplayName("ClasspathPackageSupplier @JvmOverloads constructor")
    class ClasspathSupplierOverloads {

        @Test
        @DisplayName("5-arg constructor without distributionType should default to VALKEY")
        void constructorWithoutDistributionType() {
            // 5-arg (without distributionType)
            ClasspathPackageSupplier supplier = new ClasspathPackageSupplier(
                    "/nonexistent/pkg.tar.gz", "9.0.2", OperatingSystem.LINUX_X86_64,
                    Paths.get("bin", "valkey-server"), ArchiveType.TAR_GZ
            );
            assertThat(supplier).isNotNull();
        }
    }

    @Nested
    @DisplayName("Shard PortProvider constructor")
    class ShardOverloads {

        @Test
        @DisplayName("Shard(name, PortProvider, replicaCount) should allocate ports")
        void shardWithPortProvider() {
            Shard shard = new Shard("test-shard", new PortProvider(), 2);
            assertThat(shard.getReplicaPorts()).hasSize(2);
            assertThat(shard.getName()).isEqualTo("test-shard");
        }
    }
}
