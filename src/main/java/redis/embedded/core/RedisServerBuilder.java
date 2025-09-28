package redis.embedded.core;

import io.github.tobi.laa.embedded.valkey.distribution.ValkeyDistributionProvider;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static io.github.tobi.laa.embedded.valkey.distribution.ValkeyDistroProvidersKt.DEFAULT_PROVIDERS;
import static io.github.tobi.laa.embedded.valkey.operatingsystem.DetectOperatingSystemKt.detectOperatingSystem;
import static java.nio.charset.StandardCharsets.UTF_8;
import static redis.embedded.Redis.DEFAULT_REDIS_PORT;

public final class RedisServerBuilder {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private ValkeyDistributionProvider distributionProvider = DEFAULT_PROVIDERS.get(detectOperatingSystem());
    private String bindAddress = "127.0.0.1";
    private int bindPort = DEFAULT_REDIS_PORT;
    private InetSocketAddress slaveOf;
    private boolean forceStop = false;
    private Consumer<String> soutListener;
    private Consumer<String> serrListener;

    private StringBuilder redisConfigBuilder = new StringBuilder();

    public RedisServerBuilder distributionProvider(final ValkeyDistributionProvider distributionProvider) {
        this.distributionProvider = distributionProvider;
        return this;
    }

    public RedisServerBuilder bind(final String bind) {
        this.bindAddress = bind;
        return this;
    }

    public RedisServerBuilder port(final int port) {
        this.bindPort = port;
        return this;
    }

    public RedisServerBuilder slaveOf(final String hostname, final int port) {
        this.slaveOf = new InetSocketAddress(hostname, port);
        return this;
    }

    public RedisServerBuilder slaveOf(final InetSocketAddress slaveOf) {
        this.slaveOf = slaveOf;
        return this;
    }

    public RedisServerBuilder configFile(final String redisConf) throws IOException {
        return configFile(Paths.get(redisConf));
    }

    public RedisServerBuilder configFile(final Path redisConf) throws IOException {
        Files.lines(redisConf).forEach(line -> redisConfigBuilder.append(line).append(LINE_SEPARATOR));
        return this;
    }

    public RedisServerBuilder settingIf(final boolean shouldSet, final String configLine) {
        if (shouldSet) setting(configLine);
        return this;
    }

    public RedisServerBuilder setting(final String configLine) {
        redisConfigBuilder.append(configLine).append(LINE_SEPARATOR);
        return this;
    }

    public RedisServerBuilder onShutdownForceStop(final boolean forceStop) {
        this.forceStop = forceStop;
        return this;
    }

    public RedisServerBuilder soutListener(final Consumer<String> soutListener) {
        this.soutListener = soutListener;
        return this;
    }

    public RedisServerBuilder serrListener(final Consumer<String> serrListener) {
        this.serrListener = serrListener;
        return this;
    }

    public RedisServer build() throws IOException {
        return new RedisServer(bindPort, buildCommandArgs(), forceStop, soutListener, serrListener);
    }

    public void reset() {
        this.slaveOf = null;
    }

    public List<String> buildCommandArgs() throws IOException {
        setting("bind " + bindAddress);

        final Path redisConfigFile =
                writeNewRedisConfigFile("embedded-redis-server_" + bindPort, redisConfigBuilder.toString());

        final var valkeyDistribution = distributionProvider.provideDistribution();

        final List<String> args = new ArrayList<>();
        args.add(valkeyDistribution.getBinaryPath().toAbsolutePath().toString());
        args.add(redisConfigFile.toAbsolutePath().toString());
        args.add("--port");
        args.add(Integer.toString(bindPort));

        if (slaveOf != null) {
            args.add("--slaveof");
            args.add(slaveOf.getHostName());
            args.add(Integer.toString(slaveOf.getPort()));
        }

        return args;
    }

    private static Path writeNewRedisConfigFile(final String prefix, final String contents) throws IOException {
        final Path redisConfigFile = Files.createTempFile(prefix, ".conf");
        redisConfigFile.toFile().deleteOnExit();
        Files.write(redisConfigFile, contents.getBytes(UTF_8));
        return redisConfigFile;
    }

}
