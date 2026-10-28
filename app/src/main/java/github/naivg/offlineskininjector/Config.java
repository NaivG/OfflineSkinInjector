package github.naivg.offlineskininjector;

import java.io.File;
import java.util.Locale;

/**
 * Agent configuration, read from system properties.
 *
 * <ul>
 *   <li>{@code -Dofflineskininjector.skin=C:\path\skin.png} — the PNG to serve (required)</li>
 *   <li>{@code -Dofflineskininjector.cape=C:\path\cape.png} — the cape PNG to serve (optional,
 *       64x32); the client also reuses it for elytra</li>
 *   <li>{@code -Dofflineskininjector.mode=self|all} — replace only the local player (default) or
 *       everyone</li>
 *   <li>{@code -Dofflineskininjector.model=auto|classic|slim} — the model metadata of the
 *       texture (default auto: detect the 4px/3px arm layout from a 64x64 skin)</li>
 *   <li>{@code -Dofflineskininjector.format=auto|64x64|64x32} — layout of the served PNG
 *       (default auto: 64x32 for clients 1.7.x and older, 64x64 for 1.8+)</li>
 *   <li>{@code -Dofflineskininjector.uuid=&lt;32 hex&gt;} — local player UUID (optional,
 *       auto-detected)</li>
 *   <li>{@code -Dofflineskininjector.name=&lt;name&gt;} — local player name (optional,
 *       auto-detected)</li>
 * </ul>
 */
public final class Config {

    public enum Mode {
        SELF, ALL;

        static Mode parse(String raw) {
            if (raw == null) {
                return SELF;
            }
            String v = raw.trim().toLowerCase(Locale.ROOT);
            return "all".equals(v) ? ALL : SELF;
        }
    }

    /** Layout of the PNG handed to the client. */
    public enum Format {
        AUTO, MODERN, LEGACY;

        static Format parse(String raw) {
            if (raw == null) {
                return AUTO;
            }
            String v = raw.trim().toLowerCase(Locale.ROOT);
            if (v.isEmpty() || "auto".equals(v)) {
                return AUTO;
            }
            if ("64x64".equals(v) || "modern".equals(v)) {
                return MODERN;
            }
            if ("64x32".equals(v) || "legacy".equals(v)) {
                return LEGACY;
            }
            throw new IllegalArgumentException(
                    "-Dofflineskininjector.format must be auto, 64x64 or 64x32");
        }
    }

    private final File skinFile;
    private final File capeFile;
    private final Mode mode;
    private final SkinModel model;
    private final Format format;
    private final String uuid;
    private final String name;

    private Config(File skinFile, File capeFile, Mode mode, SkinModel model, Format format,
                   String uuid, String name) {
        this.skinFile = skinFile;
        this.capeFile = capeFile;
        this.mode = mode;
        this.model = model;
        this.format = format;
        this.uuid = uuid;
        this.name = name;
    }

    public static Config load() {
        String skin = System.getProperty("offlineskininjector.skin");
        if (skin == null || skin.trim().isEmpty()) {
            throw new IllegalArgumentException("-Dofflineskininjector.skin=<file.png> is required");
        }
        File file = new File(skin.trim());
        if (!file.isFile()) {
            throw new IllegalArgumentException("skin file not found: " + file.getAbsolutePath());
        }

        // A broken cape path only disables the cape.
        File capeFile = parseOptionalFile(System.getProperty("offlineskininjector.cape"));

        Mode mode = Mode.parse(System.getProperty("offlineskininjector.mode"));

        SkinModel model = SkinModel.parse(System.getProperty("offlineskininjector.model"));

        Format format = Format.parse(System.getProperty("offlineskininjector.format"));

        String uuid = normalizeUuid(System.getProperty("offlineskininjector.uuid"));
        String name = trimToNull(System.getProperty("offlineskininjector.name"));

        // The launcher command line carries --uuid and --username.
        String[] launcher = parseLauncherArguments();
        if (uuid == null) {
            uuid = launcher[0];
        }
        if (name == null) {
            name = launcher[1];
        }

        return new Config(file, capeFile, mode, model, format, uuid, name);
    }

    public File skinFile() {
        return skinFile;
    }

    /** The cape PNG to serve, or {@code null} when capes are not replaced. */
    public File capeFile() {
        return capeFile;
    }

    public Mode mode() {
        return mode;
    }

    /** The requested model; {@link SkinModel#AUTO} means detect it from the skin. */
    public SkinModel model() {
        return model;
    }

    public Format format() {
        return format;
    }

    /** 32 lowercase hex characters, or {@code null} when it could not be determined. */
    public String uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    private static File parseOptionalFile(String raw) {
        String value = trimToNull(raw);
        return value == null ? null : new File(value);
    }

    /** Reads {@code --uuid} and {@code --username} from the launcher command line. */
    private static String[] parseLauncherArguments() {
        String command = System.getProperty("sun.java.command");
        if (command == null || command.isEmpty()) {
            return new String[]{null, null};
        }
        String[] tokens = command.split("\\s+");
        String uuid = null;
        String name = null;
        for (int i = 0; i < tokens.length - 1; i++) {
            String token = tokens[i];
            if ("--uuid".equals(token)) {
                uuid = normalizeUuid(tokens[i + 1]);
            } else if ("--username".equals(token) || "--name".equals(token)) {
                name = trimToNull(tokens[i + 1]);
            }
        }
        return new String[]{uuid, name};
    }

    /** Accepts 32 hex chars, with or without dashes; returns null when unusable. */
    public static String normalizeUuid(String raw) {
        if (raw == null) {
            return null;
        }
        String hex = raw.trim().replace("-", "").toLowerCase(Locale.ROOT);
        if (hex.length() != 32) {
            return null;
        }
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!ok) {
                return null;
            }
        }
        return hex;
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        return v.isEmpty() ? null : v;
    }
}
