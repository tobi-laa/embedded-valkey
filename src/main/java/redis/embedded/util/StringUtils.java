package redis.embedded.util;

public final class StringUtils {

    private StringUtils() {
        // utility class
    }

    public static boolean isBlank(final String string) {
        return string == null || string.isEmpty() || string.trim().isEmpty();
    }

    public static boolean isNotBlank(final String string) {
        return !isBlank(string);
    }
}
