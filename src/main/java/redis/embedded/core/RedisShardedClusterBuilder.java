package redis.embedded.core;

import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandalone;
import io.github.tobi.laa.embedded.valkey.standalone.ValkeyStandaloneBuilder;
import redis.embedded.Redis;
import redis.embedded.RedisShardedCluster;
import redis.embedded.model.Shard;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static redis.embedded.core.PortProvider.newEphemeralPortProviderInRedisClusterRange;
import static redis.embedded.core.PortProvider.newPredefinedPortProvider;
import static redis.embedded.core.PortProvider.newSequencePortProvider;

public final class RedisShardedClusterBuilder {

    private static final Duration DEFAULT_INITIALIZATION_TIMEOUT = Duration.ofSeconds(20);

    private ValkeyStandaloneBuilder serverBuilder = new ValkeyStandaloneBuilder();
    private PortProvider shardPortProvider = newSequencePortProvider(6379);
    private Duration initializationTimeout = DEFAULT_INITIALIZATION_TIMEOUT;
    private final List<Shard> shards = new LinkedList<>();
    private final Map<Integer, Set<Integer>> replicasPortsByMainNodePort = new LinkedHashMap<>();

    public RedisShardedClusterBuilder withServerBuilder(final ValkeyStandaloneBuilder serverBuilder) {
        this.serverBuilder = serverBuilder;
        return this;
    }

    public RedisShardedClusterBuilder serverPorts(final Collection<Integer> ports) {
        this.shardPortProvider = newPredefinedPortProvider(ports);
        return this;
    }

    public RedisShardedClusterBuilder initializationTimeout(final Duration initializationTimeout) {
        this.initializationTimeout = initializationTimeout;
        return this;
    }

    public RedisShardedClusterBuilder ephemeralServers() {
        this.shardPortProvider = newEphemeralPortProviderInRedisClusterRange();
        return this;
    }

    public RedisShardedClusterBuilder ephemeral() {
        ephemeralServers();
        return this;
    }

    public RedisShardedClusterBuilder shard(final String name, final int replicaCount) {
        this.shards.add(new Shard(name, replicaCount, this.shardPortProvider));
        return this;
    }

    public RedisShardedCluster build() throws IOException {
        final List<Redis> servers = buildServers();
        return new RedisShardedCluster(servers, replicasPortsByMainNodePort, initializationTimeout);
    }

    private List<Redis> buildServers() throws IOException {
        final List<Redis> servers = new ArrayList<>();
        for (final Shard shard : shards) {
            servers.add(buildMainNode(shard));
            servers.addAll(buildReplicas(shard));
        }
        return servers;
    }

    private List<ValkeyStandalone> buildReplicas(final Shard shard) throws IOException {
        final List<ValkeyStandalone> replicas = new ArrayList<>();
        final Set<Integer> replicaPorts = replicasPortsByMainNodePort.get(shard.mainNodePort);
        for (final Integer replicaPort : shard.replicaPorts) {
            replicaPorts.add(replicaPort);
            var replicaBuilder = serverBuilder.clone();
            replicaBuilder.port(replicaPort);
            replicaBuilder.directive("cluster-enabled", "yes");
            replicaBuilder.directive("cluster-config-file", "nodes-replica-" + replicaPort + ".conf");
            replicaBuilder.directive("cluster-node-timeout", "5000");
            replicaBuilder.directive("appendonly", "no");
            final ValkeyStandalone slave = replicaBuilder.build();
            replicas.add(slave);
        }
        return replicas;
    }

    private ValkeyStandalone buildMainNode(final Shard shard) throws IOException {
        replicasPortsByMainNodePort.put(shard.mainNodePort, new HashSet<>());
        var mainNodeBuilder = serverBuilder.clone();
        mainNodeBuilder.directive("cluster-enabled", "yes");
        mainNodeBuilder.directive("cluster-config-file", "nodes-main-" + shard.mainNodePort + ".conf");
        mainNodeBuilder.directive("cluster-node-timeout", "5000");
        mainNodeBuilder.directive("appendonly", "no");
        return mainNodeBuilder.port(shard.mainNodePort).build();
    }

}
