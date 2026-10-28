package github.naivg.offlineskininjector;

import java.awt.image.BufferedImage;
import java.lang.instrument.Instrumentation;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent entry point.
 *
 * <p>Reads the configuration, validates the PNGs, converts the skin to the client's layout,
 * starts the loopback HTTP server and installs the {@link AuthlibTransformer}. This runs in
 * {@code premain}, before the client downloads any skin.
 */
public final class Premain {

    private static final String PREFIX = "[OfflineSkinInjector] ";

    private Premain() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        start(agentArgs, instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        start(agentArgs, instrumentation);
    }

    private static void start(String agentArgs, Instrumentation instrumentation) {
        try {
            Config config = Config.load();
            SourceTexture source = SourceTexture.readSkin(config.skinFile());

            SkinModel requestedModel = config.model();
            SkinModel model = requestedModel == SkinModel.AUTO
                    ? SkinModelDetector.detect(source.image())
                    : requestedModel;

            String clientVersion = ClientVersion.detect();
            boolean legacy = isLegacyLayout(config, clientVersion);

            BufferedImage servedImage = legacy
                    ? SkinConverter.toLegacy(source.image())
                    : SkinConverter.toModern(source.image());
            byte[] png = servedImage == source.image()
                    ? source.bytes()
                    : SkinConverter.encodePng(servedImage);
            String sha1 = sha1Hex(png);

            String skinPath = "/skin/" + sha1 + ".png";
            Map<String, byte[]> resources = new HashMap<String, byte[]>();
            resources.put(skinPath, png);

            String capePath = null;
            if (config.capeFile() != null) {
                try {
                    SourceTexture cape = SourceTexture.readCape(config.capeFile());
                    capePath = "/cape/" + sha1Hex(cape.bytes()) + ".png";
                    resources.put(capePath, cape.bytes());
                } catch (Throwable t) {
                    // A broken cape must never take the skin down.
                    System.out.println(PREFIX + "  warning  = cape disabled: " + t);
                    if (Boolean.getBoolean("offlineskininjector.debug")) {
                        t.printStackTrace();
                    }
                }
            }

            TextureHttpServer server = TextureHttpServer.start(resources);
            String capeUrl = capePath == null ? null : server.url(capePath);

            TextureSet textures = new TextureSet(server.url(skinPath), capeUrl, model.metadata());
            Hooks.init(config, textures);

            instrumentation.addTransformer(new AuthlibTransformer());

            System.out.println(PREFIX + "Successfully initialize injector.");
            System.out.println(PREFIX + "  skin     = " + config.skinFile().getAbsolutePath());
            System.out.println(PREFIX + "  source   = " + source.width() + "x" + source.height());
            System.out.println(PREFIX + "  format   = " + servedImage.getWidth() + "x" + servedImage.getHeight()
                    + " (" + describeFormat(config, clientVersion) + ")");
            System.out.println(PREFIX + "  url      = " + server.url(skinPath));
            String capeDescription = "<none>";
            if (capeUrl != null) {
                capeDescription = config.capeFile().getAbsolutePath();
            } else if (config.capeFile() != null) {
                capeDescription = "<disabled>";
            }
            System.out.println(PREFIX + "  cape     = " + capeDescription);
            if (capeUrl != null) {
                System.out.println(PREFIX + "  cape url = " + capeUrl);
            }
            System.out.println(PREFIX + "  mode     = " + config.mode().name().toLowerCase(java.util.Locale.ROOT));
            System.out.println(PREFIX + "  model    = " + model.metadata()
                    + " (" + describeModel(requestedModel, model) + ")");
            System.out.println(PREFIX + "  uuid     = " + (config.uuid() == null ? "<unknown>" : config.uuid()));
            System.out.println(PREFIX + "  name     = " + (config.name() == null ? "<unknown>" : config.name()));

            if (model == SkinModel.SLIM) {
                if (source.height() == SkinConverter.LEGACY_HEIGHT) {
                    System.out.println(PREFIX
                            + "  warning  = a 64x32 skin is classic; model=slim will look wrong");
                } else if (legacy) {
                    System.out.println(PREFIX
                            + "  warning  = 1.7.x has no slim model; the arms will look wrong");
                }
            }
        } catch (Throwable t) {
            // Never prevent the game from starting.
            System.out.println(PREFIX + "disabled: " + t);
            if (Boolean.getBoolean("offlineskininjector.debug")) {
                t.printStackTrace();
            }
        }
    }

    private static boolean isLegacyLayout(Config config, String clientVersion) {
        if (config.format() == Config.Format.LEGACY) {
            return true;
        }
        if (config.format() == Config.Format.MODERN) {
            return false;
        }
        return ClientVersion.isLegacy(clientVersion);
    }

    private static String describeFormat(Config config, String clientVersion) {
        if (config.format() != Config.Format.AUTO) {
            return "forced";
        }
        if (clientVersion == null) {
            return "auto, client version unknown";
        }
        return "auto, client " + clientVersion;
    }

    private static String describeModel(SkinModel requested, SkinModel resolved) {
        if (requested != SkinModel.AUTO) {
            return "explicit";
        }
        int armWidth = resolved == SkinModel.SLIM ? 3 : 4;
        return "auto, " + armWidth + "px arms";
    }

    /** The URL contains the content hash, so a new texture always produces a new URL. */
    private static String sha1Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
