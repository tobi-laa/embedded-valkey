package redis.embedded;

import io.github.tobi.laa.embedded.valkey.distribution.ValkeyDistributionProvider;
import redis.embedded.core.RedisServerBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class RedisServer extends RedisInstance {

    private static Path provideDistroAndReturnExecutable(ValkeyDistributionProvider valkeyDistributionProvider) throws IOException {
        final var distro = valkeyDistributionProvider.provideDistribution();
        return distro.getBinaryPath();
    }

    public RedisServer() throws IOException {
        this(DEFAULT_REDIS_PORT);
    }

    public RedisServer(final int port) throws IOException {
        this(port, newRedisServer().port(port).buildCommandArgs(), false);
    }

    public RedisServer(final int port, final File executable) {
        this(port
                , Arrays.asList(executable.getAbsolutePath(), "--port", Integer.toString(port))
                , false
        );
    }

    public RedisServer(final int port, final ValkeyDistributionProvider valkeyDistributionProvider) throws IOException {
        this(port
                , Arrays.asList(provideDistroAndReturnExecutable(valkeyDistributionProvider).toAbsolutePath().toString(), "--port", Integer.toString(port))
                , false
        );
    }

    public RedisServer(final int port, final List<String> args, final boolean forceStop) {
        super(port, args, SERVER_READY_PATTERN, forceStop, null, null);
    }

    public RedisServer(final int port, final List<String> args, final boolean forceStop,
                       final Consumer<String> soutListener, final Consumer<String> serrListener) {
        super(port, args, SERVER_READY_PATTERN, forceStop, soutListener, serrListener);
    }

    public static RedisServerBuilder newRedisServer() {
        return new RedisServerBuilder();
    }

}
