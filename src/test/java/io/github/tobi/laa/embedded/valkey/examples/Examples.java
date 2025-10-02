package io.github.tobi.laa.embedded.valkey.examples;

import io.github.tobi.laa.embedded.valkey.ValkeyNode;
import io.github.tobi.laa.embedded.valkey.cluster.highavailability.ValkeyHighAvailability;
import io.github.tobi.laa.embedded.valkey.cluster.sharded.ValkeyShardedCluster;
import io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSupplier;
import io.github.tobi.laa.embedded.valkey.operatingsystem.OperatingSystem;
import io.github.tobi.laa.embedded.valkey.sentinel.ValkeySentinel;
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Paths;
import java.util.List;

import static io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt.downloadAndInstallMacOsPackageFromMacports;
import static io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt.installValkeyIoLinuxPackageFromClasspath;
import static io.github.tobi.laa.embedded.valkey.installation.ValkeyInstallationSuppliersKt.installWinX64MemuraiPackageFromClasspath;

// Please note that these examples are included in the README.adoc by using the line numbers!
// @formatter:off
@SuppressWarnings("all") // this class solely serves documentation purposes
class Examples {

private void simpleValkeyStandaloneExample() throws IOException {
ValkeyStandalone valkey = ValkeyStandalone.builder().build();
valkey.start();
// do some work
valkey.stop();
}

private void valkeyStandaloneWithCustomInstallationSuppliersExample() throws IOException {
ValkeyStandalone valkey = ValkeyStandalone.builder()
        .installationSupplier(OperatingSystem.MAC_OS_ARM64, downloadAndInstallMacOsPackageFromMacports(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("my.proxy.com", 8080)), OperatingSystem.MAC_OS_ARM64))
        .installationSupplier(OperatingSystem.LINUX_X86_64, installValkeyIoLinuxPackageFromClasspath("/bundles/valkey-8.1.3-jammy-x86_64.tar.gz", OperatingSystem.LINUX_X86_64))
        .installationSupplier(OperatingSystem.WINDOWS_X86_64, installWinX64MemuraiPackageFromClasspath("/bundles/memuraideveloper.4.1.6.nupkg"))
        .build();
}

private void simpleValkeyStandaloneFluentConfigExample() throws IOException {
ValkeyInstallationSupplier customInstallationProvider = null;
ValkeyStandalone valkey = ValkeyStandalone.builder()
        .installationSupplier(OperatingSystem.MAC_OS_ARM64, customInstallationProvider)
        .port(6379)
        .replicaOf("locahost", 6378)
        .build();
}

private void advancedValkeyStandaloneFluentConfigExample() throws IOException {
ValkeyInstallationSupplier customInstallationProvider = null;
ValkeyStandalone valkey = ValkeyStandalone.builder()
        .installationSupplier(OperatingSystem.MAC_OS_ARM64, customInstallationProvider)
        .importConf(Paths.get("/path/to/your/base/valkey.conf"))
        // everything that follows will override settings in the imported configuration
        .bind("127.0.0.1") // good for local development on Windows to prevent security popups
        .port(6379)
        .replicaOf("locahost", 6378)
        .directive("daemonize", "no")
        .directive("appendonly", "no")
        .directive("maxmemory", "128M")
        .build();
}

private void valkeyHighAvailClusterExample() throws IOException {
//creates a cluster with 3 sentinels, quorum size of 2 and 3 replication groups, each with one main node and one replica
ValkeyHighAvailability valkey = ValkeyHighAvailability.builder()
        .withSentinelBuilder(ValkeySentinel.builder())
        .sentinelCount(3)
        .quorumSize(2)
        .replicationGroup("main1", 1)
        .replicationGroup("main2", 1)
        .replicationGroup("main3", 1)
        .build();
valkey.start();
// do some work
valkey.stop();
}

private void valkeyShardedClusterExample() throws IOException {
ValkeyShardedCluster valkey = ValkeyShardedCluster.builder()
        .shard("main1", 1)
        .shard("main2", 1)
        .shard("main3", 1)
        .build();
valkey.start();
// do some work
valkey.stop();
}

private void retrievePortsExample() throws IOException {
ValkeyStandalone standalone = ValkeyStandalone.builder().build();
standalone.start();
int port = standalone.getPort(); // works the same way for sentinels

ValkeyHighAvailability highAvail = ValkeyHighAvailability.builder().build();
highAvail.start();
List<Integer> sentinelPorts = highAvail.getSentinels().stream().map(ValkeyNode::getPort).toList();

ValkeyShardedCluster cluster = ValkeyShardedCluster.builder()
        .shard("main1", 1)
        .shard("main2", 1)
        .shard("main3", 1)
        .build();
cluster.start();
List<Integer> mainNodePorts = cluster.getMainNodes().stream().map(ValkeyNode::getPort).toList();
List<Integer> replicaPorts = cluster.getReplicas().stream().map(ValkeyNode::getPort).toList();
}
}
// @formatter:on
