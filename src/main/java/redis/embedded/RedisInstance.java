package redis.embedded;


import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfLocator;
import io.github.tobi.laa.embedded.valkey.conf.ValkeyConfParser;
import io.github.tobi.laa.embedded.valkey.conf.ValkeyDirective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.util.IO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static redis.embedded.util.IO.addShutdownHook;
import static redis.embedded.util.IO.checkedToRuntime;
import static redis.embedded.util.IO.findMatchInStream;
import static redis.embedded.util.IO.logStream;
import static redis.embedded.util.IO.newDaemonThread;
import static redis.embedded.util.IO.readFully;

public abstract class RedisInstance implements Redis {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisInstance.class);

    private final Pattern readyPattern;
    private final int port;
    private final List<String> args;
    private final boolean forceStop;
    private final Consumer<String> soutListener;
    private final Consumer<String> serrListener;

    private volatile boolean active = false;
    private Process process;

    protected RedisInstance(final int port, final List<String> args, final Pattern readyPattern,
                            final boolean forceStop, final Consumer<String> soutListener,
                            final Consumer<String> serrListener) {
        this.port = port;
        this.args = args;
        this.readyPattern = readyPattern;
        this.forceStop = forceStop;
        this.soutListener = soutListener;
        this.serrListener = serrListener;
    }

    public synchronized void start() throws IOException {
        if (active) return;
        final File executable = new File(args.get(0));

        if (!executable.isFile())
            throw new FileNotFoundException("Redis binary " + args.get(0) + " could not be found");
        if (!executable.canExecute())
            throw new AccessDeniedException("Redis binary " + args.get(0) + " is not executable");

        try {
            Files.createDirectories(getRedisDir());
            process = new ProcessBuilder(args)
                    .directory(getRedisDir().toFile())
                    .start();
            addShutdownHook("RedisInstanceCleaner", checkedToRuntime(this::stop));
            awaitServerReady(process, readyPattern, soutListener, serrListener);

            if (serrListener != null)
                newDaemonThread(() -> logStream(process.getErrorStream(), serrListener)).start();
            if (soutListener != null)
                newDaemonThread(() -> logStream(process.getInputStream(), soutListener)).start();

            active = true;
        } catch (final IOException e) {
            throw new IOException("Failed to start Redis service", e);
        }
    }

    private Path getRedisDir() {
        return Paths.get(args.get(0)).getParent().resolve("data_dir_" + port);
    }

    // You might get an error when you try to start the default binary without having openssl installed. The default
    // binaries have TLS support but require a library on the host OS. On MacOS you will probably get an error that
    // looks like this:
    //
    //     '/opt/homebrew/opt/openssl@3/lib/libssl.3.dylib' (no such file),
    //     '/System/Volumes/Preboot/Cryptexes/OS/opt/homebrew/opt/openssl@3/lib/libssl.3.dylib' (no such file),
    //     '/opt/homebrew/opt/openssl@3/lib/libssl.3.dylib' (no such file),
    //     '/usr/lib/libssl.3.dylib' (no such file, not in dyld cache)
    //
    // One option for resolving the issue is to install openssl using `brew install openssl@3`. Alternatively, you
    // can use a binary that doesn't have TLS support. Either by compiling your own from source, or by using HankCP's
    // binary at ExecutableProvider.REDIS_7_2_MACOSX_14_SONOMA_HANKCP, or downloading one from some other place.
    //
    // On linux the error will look like this:
    //
    //     /app/redis-server-6.2.6-v5-linux-amd64: error while loading shared libraries: libssl.so.3: cannot open
    //     shared object file: No such file or directory
    //
    // The problem is the same as on MacOS. You need a binary that doesn't require the libssl library or you need to
    // provide that library. If you are running the app on your host you can install the needed package using your
    // package manager. Such as with apt-get (sudo apt-get install libssl1.0.0 libssl-dev). If you are running this
    // inside a docker image you'll need to make sure the library is available inside the image.

    private static void awaitServerReady(final Process process, final Pattern readyPattern
            , final Consumer<String> soutListener, final Consumer<String> serrListener) throws IOException {
        final StringBuilder log = new StringBuilder();
        if (!findMatchInStream(process.getInputStream(), readyPattern, soutListener, log)) {
            final String stdOut = log.toString();
            final String stdErr = readFully(process.getErrorStream(), serrListener);

            throw new IOException("Redis-server process appears not to have started. "
                    + (isNullOrEmpty(stdOut) ? "No output was found in standard-out." : "stdandard-out contains this: " + stdOut)
                    + " "
                    + (isNullOrEmpty(stdErr) ? "No output was found in standard-err." : "stdandard-err contains this: " + stdErr)
            );
        }
    }

    private static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    public synchronized void stop() throws IOException {
        if (!active) return;

        try {
            if (forceStop)
                process.destroyForcibly();
            else {
                process.destroy();
                process.waitFor();
            }
            active = false;
        } catch (final InterruptedException e) {
            throw new IOException("Failed to stop redis service", e);
        } finally {
            findAndDeleteDumpRdbFile();
            findAndDeleteClusterConfigFiles();
        }
    }

    private void findAndDeleteDumpRdbFile() {
        final Path dumpRdb = getRedisDir().resolve("dump.rdb");
        IO.deleteSafely(dumpRdb);
    }

    private void findAndDeleteClusterConfigFiles() {
        findAndDeleteClusterConfigFiles(findRedisConfigFile());
    }

    private Path findRedisConfigFile() {
        return ValkeyConfLocator.INSTANCE.locate(this);
    }

    private void findAndDeleteClusterConfigFiles(final Path redisConfig) {
        try {
            final var config = ValkeyConfParser.INSTANCE.parse(redisConfig);
            config.directives("cluster-config-file")
                    .stream()
                    .map(ValkeyDirective::getArguments)
                    .flatMap(List::stream)
                    .map(clusterConfFile -> getRedisDir().resolve(clusterConfFile))
                    .forEach(IO::deleteSafely);
        } catch (IOException e) {
            LOGGER.error("Unable to parse redis config file", e);
        }
    }

    public boolean active() {
        return active;
    }

    public List<Integer> ports() {
        return Collections.singletonList(port);
    }

}
