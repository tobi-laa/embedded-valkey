package redis.embedded.util;

import io.github.tobi.laa.embedded.valkey.cluster.highavailability.ValkeyHighAvailability;
import redis.embedded.Redis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum JedisUtil {
    ;

    public static Set<String> jedisHosts(final Redis redis) {
        return portsToJedisHosts(redis.ports());
    }

    public static Set<String> sentinelHosts(final ValkeyHighAvailability cluster) {
        return portsToJedisHosts(cluster.sentinelPorts());
    }

    public static Set<String> portsToJedisHosts(final List<Integer> ports) {
        final Set<String> hosts = new HashSet<>();
        for (final Integer p : ports) {
            hosts.add("localhost:" + p);
        }
        return hosts;
    }

}
