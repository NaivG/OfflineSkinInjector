package github.naivg.offlineskininjector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the Minecraft version used by the running client.
 *
 * <p>The launcher's {@code --version} or {@code --assetIndex} argument is used first, then the
 * {@code versions/<id>} entry in the class path. Unknown versions are treated as modern; a
 * 1.7.x client crops a 64x64 skin to 64x32 on its own.
 */
final class ClientVersion {

    private static final Pattern VERSION_ARG = Pattern.compile("--version(?:\\s+|=)(\\S+)");
    private static final Pattern ASSET_INDEX_ARG = Pattern.compile("--assetIndex(?:\\s+|=)(\\S+)");
    private static final Pattern CLASSPATH_VERSION =
            Pattern.compile("[\\\\/]versions[\\\\/]([^\\\\/]+)[\\\\/]");
    private static final Pattern RELEASE = Pattern.compile("^[vV]?(\\d+)\\.(\\d+)");
    private static final Pattern SNAPSHOT = Pattern.compile("^(\\d{2})w\\d{2}[a-z]$");

    private ClientVersion() {
    }

    /** The Minecraft version id as passed by the launcher, or {@code null} when unknown. */
    static String detect() {
        String command = System.getProperty("sun.java.command");
        String version = fromCommand(command);
        if (version == null) {
            version = fromClasspath(System.getProperty("java.class.path"));
        }
        return version;
    }

    /** Extracts the {@code --version} value, falling back to {@code --assetIndex}. */
    static String fromCommand(String command) {
        if (command == null || command.isEmpty()) {
            return null;
        }
        Matcher version = VERSION_ARG.matcher(command);
        if (version.find()) {
            return stripQuotes(version.group(1));
        }
        Matcher assetIndex = ASSET_INDEX_ARG.matcher(command);
        return assetIndex.find() ? stripQuotes(assetIndex.group(1)) : null;
    }

    private static String stripQuotes(String value) {
        if (value != null && value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    /** Extracts the version id from a class path entry such as {@code .../versions/1.7.10/1.7.10.jar}. */
    static String fromClasspath(String classpath) {
        if (classpath == null || classpath.isEmpty()) {
            return null;
        }
        Matcher matcher = CLASSPATH_VERSION.matcher(classpath);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** Whether the version uses the 64x32 skin layout. Unknown versions are treated as modern. */
    static boolean isLegacy(String version) {
        if (version == null) {
            return false;
        }
        String value = version.trim();
        Matcher release = RELEASE.matcher(value);
        if (release.find()) {
            int major = Integer.parseInt(release.group(1));
            int minor = Integer.parseInt(release.group(2));
            return major == 0 || (major == 1 && minor <= 7);
        }
        // 14w and newer snapshots use the modern layout.
        Matcher snapshot = SNAPSHOT.matcher(value);
        if (snapshot.find()) {
            return Integer.parseInt(snapshot.group(1)) <= 13;
        }
        return false;
    }
}
