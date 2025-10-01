package redis.embedded;

import io.github.tobi.laa.embedded.valkey.cluster.sharded.ValkeyShardedCluster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.io.IOException;
import java.nio.file.Path;

import static io.github.tobi.laa.embedded.valkey.cluster.sharded.ValkeyShardedCluster.builder;
import static org.junit.Assert.assertEquals;

class ValkeyShardedClusterTest {

    @TempDir
    static Path temporaryFolder;

    private ValkeyShardedCluster cluster;

    @AfterEach
    void stopCluster() throws IOException {
        if (cluster != null) {
            cluster.stop();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        cluster = builder()
                .shard("master1", 1)
                .shard("master2", 1)
                .shard("master3", 1)
                .build();
        cluster.start();
    }

    @Test
    void testSimpleOperationsAfterClusterStart() {
        try (final JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", cluster.getNodes().get(0).getPort()))) {
            jc.set("somekey", "somevalue");
            assertEquals("the value should be equal", "somevalue", jc.get("somekey"));
        }
    }

    @Test
    void testSimpleOperationsAfterClusterWithEphemeralPortsStart() throws IOException {
        cluster.stop();
        cluster = builder()
                .shard("master1", 1)
                .shard("master2", 1)
                .shard("master3", 1)
                .build();
        cluster.start();
        try (final JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", cluster.getNodes().get(0).getPort()))) {
            jc.set("somekey", "somevalue");
            assertEquals("the value should be equal", "somevalue", jc.get("somekey"));
        }
    }

    @Test
    void shouldAllowSubsequentRuns() throws IOException {
        cluster.stop();
        cluster.start();
        try (final JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", cluster.getNodes().get(0).getPort()))) {
            jc.set("somekey", "somevalue");
            assertEquals("the value should be equal", "somevalue", jc.get("somekey"));
        }
    }

//    @Test
//    public void shouldAllowSubsequentRunsInSameDirectory() throws IOException {
//        cluster.stop();
//        //
//        File folder = temporaryFolder.newFolder();
//        cluster = newRedisCluster()
//                .withServerBuilder(RedisServer
//                        .newRedisServer()
//                        .executableProvider(newJarResourceProvider(folder)))
//                .shard("master1", 1)
//                .shard("master2", 1)
//                .shard("master3", 1)
//                .build();
//        cluster.start();
//        cluster.stop();
//        cluster.start();
//        try (final JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", cluster.getPort()))) {
//            jc.set("somekey", "somevalue");
//            assertEquals("the value should be equal", "somevalue", jc.get("somekey"));
//        }
//    }
}
