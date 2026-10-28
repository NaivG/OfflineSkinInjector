package github.naivg.offlineskininjector;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime hooks called by the transformed authlib methods.
 *
 * <p>Every authlib type is resolved reflectively through the game's class loader, so the agent
 * has no compile-time dependency on authlib. {@link TextureSet} describes the textures; this
 * class turns them into the authlib objects the client expects.
 *
 * <p>Methods must never throw: the vanilla client only catches {@code InsecureTextureException}
 * around the skin lookup, so any other exception would silently break the skin load.
 */
public final class Hooks {

    private static final String TEXTURE_CLASS = "com.mojang.authlib.minecraft.MinecraftProfileTexture";
    private static final String TEXTURE_TYPE_CLASS = TEXTURE_CLASS + "$Type";
    private static final String PROPERTY_CLASS = "com.mojang.authlib.properties.Property";
    private static final String TEXTURES_CLASS = "com.mojang.authlib.minecraft.MinecraftProfileTextures";
    private static final String SIGNATURE_STATE_CLASS = "com.mojang.authlib.SignatureState";

    private static volatile TextureSet textures;
    private static volatile boolean overrideAll;
    private static volatile String localUuid;
    private static volatile String localName;

    /** Cached per class loader: the legacy texture map and the packed property. */
    private static final Map<ClassLoader, Map> LEGACY_CACHE = new ConcurrentHashMap<ClassLoader, Map>();
    private static final Map<ClassLoader, Object> PACKED_CACHE = new ConcurrentHashMap<ClassLoader, Object>();
    private static final Map<ClassLoader, Object> TEXTURES_CACHE = new ConcurrentHashMap<ClassLoader, Object>();

    private Hooks() {
    }

    /**
     * Installs the textures the agent replaces the player's own ones with.
     *
     * @param config   the agent configuration
     * @param textures the textures to inject
     */
    public static void init(Config config, TextureSet textures) {
        Hooks.textures = textures;
        overrideAll = config.mode() == Config.Mode.ALL;
        localUuid = config.uuid();
        localName = config.name();
    }

    /** The URL the skin is downloaded from, or {@code null} when nothing is installed. */
    public static String skinUrl() {
        TextureSet current = textures;
        return current == null ? null : current.skinUrl();
    }

    // ------------------------------------------------------------------ getTextures

    /**
     * Whether the given profile should get the replacement textures.
     * Called from {@code YggdrasilMinecraftSessionService#getTextures(GameProfile, boolean)}.
     */
    public static boolean shouldOverride(Object profile) {
        try {
            if (profile == null || skinUrl() == null) {
                return false;
            }
            if (overrideAll) {
                return true;
            }
            if (localUuid != null) {
                String id = profileUuid(profile);
                return id != null && id.equals(localUuid);
            }
            if (localName != null) {
                String name = profileName(profile);
                return name != null && name.equalsIgnoreCase(localName);
            }
            // Neither UUID nor name could be determined: do not guess.
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Replacement for {@code getTextures(GameProfile, boolean)}. The map holds the skin and,
     * when configured, the cape; no {@code ELYTRA} entry is needed because the client falls back
     * to the cape texture. An empty map means "no custom skin".
     */
    public static Map legacyTextures(Object profile) {
        try {
            TextureSet current = textures;
            if (profile == null || current == null || current.skinUrl() == null) {
                return Collections.emptyMap();
            }
            ClassLoader loader = profile.getClass().getClassLoader();
            Map cached = LEGACY_CACHE.get(loader);
            if (cached != null) {
                return cached;
            }
            Class<?> textureClass = Reflect.findClass(TEXTURE_CLASS, loader);
            Class<?> typeClass = Reflect.findClass(TEXTURE_TYPE_CLASS, loader);

            Constructor<?> ctor = Reflect.constructor(textureClass, String.class, Map.class);
            Map<String, String> metadata = new HashMap<String, String>();
            metadata.put("model", current.model());
            Object skin = ctor.newInstance(current.skinUrl(), metadata);

            Object skinType = Reflect.enumConstant(typeClass, "SKIN");
            if (skinType == null) {
                return Collections.emptyMap();
            }
            Map result = new HashMap();
            result.put(skinType, skin);
            if (current.hasCape()) {
                // Skip the cape when this authlib version has no CAPE constant.
                Object capeType = Reflect.enumConstant(typeClass, "CAPE");
                if (capeType != null) {
                    result.put(capeType, ctor.newInstance(current.capeUrl(),
                            new HashMap<String, String>()));
                }
            }
            LEGACY_CACHE.put(loader, result);
            return result;
        } catch (Throwable t) {
            return Collections.emptyMap();
        }
    }

    // ------------------------------------------------------------------ packed textures

    /**
     * Replacement for {@code getPackedTextures(GameProfile)}: a {@code textures} property whose
     * value is the base64 encoded textures payload pointing at our local URLs.
     */
    public static Object packedTextures(Object profile) {
        try {
            TextureSet current = textures;
            if (profile == null || current == null || current.skinUrl() == null) {
                return null;
            }
            ClassLoader loader = profile.getClass().getClassLoader();
            Object cached = PACKED_CACHE.get(loader);
            if (cached != null) {
                return cached;
            }
            Class<?> propertyClass = Reflect.findClass(PROPERTY_CLASS, loader);
            Constructor<?> ctor = Reflect.constructor(propertyClass, String.class, String.class);
            Object property = ctor.newInstance("textures", current.payload());
            PACKED_CACHE.put(loader, property);
            return property;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Whether the given property is the one we produced. */
    public static boolean isOurProperty(Object property) {
        try {
            TextureSet current = textures;
            if (property == null || current == null) {
                return false;
            }
            String value = Reflect.readString(property, "value", "getValue");
            if (value == null) {
                return false;
            }
            return value.equals(current.payload());
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Replacement for {@code unpackTextures(Property)}. Returns our textures marked SIGNED so the
     * client accepts them; the elytra slot is null and falls back to the cape texture.
     */
    public static Object ourTextures(Object property) {
        try {
            TextureSet current = textures;
            if (current == null || !isOurProperty(property)) {
                return null;
            }
            ClassLoader loader = property.getClass().getClassLoader();
            Object cached = TEXTURES_CACHE.get(loader);
            if (cached != null) {
                return cached;
            }
            Class<?> textureClass = Reflect.findClass(TEXTURE_CLASS, loader);
            Class<?> texturesClass = Reflect.findClass(TEXTURES_CLASS, loader);
            Class<?> stateClass = Reflect.findClass(SIGNATURE_STATE_CLASS, loader);

            Constructor<?> textureCtor = Reflect.constructor(textureClass, String.class, Map.class);
            Map<String, String> metadata = new HashMap<String, String>();
            metadata.put("model", current.model());
            Object skin = textureCtor.newInstance(current.skinUrl(), metadata);

            Object cape = null;
            if (current.hasCape()) {
                cape = textureCtor.newInstance(current.capeUrl(), new HashMap<String, String>());
            }

            Object signed = Reflect.enumConstant(stateClass, "SIGNED");
            Constructor<?> texturesCtor = Reflect.constructor(
                    texturesClass, textureClass, textureClass, textureClass, stateClass);
            // elytra = null: vanilla falls back to the cape texture.
            Object result = texturesCtor.newInstance(skin, cape, null, signed);
            TEXTURES_CACHE.put(loader, result);
            return result;
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String profileUuid(Object profile) {
        // GameProfile exposes either getId() or id().
        Object uuid = Reflect.readString(profile, "getId", "id");
        if (uuid == null) {
            return null;
        }
        return Config.normalizeUuid(String.valueOf(uuid));
    }

    private static String profileName(Object profile) {
        Object name = Reflect.readString(profile, "getName", "name");
        return name == null ? null : String.valueOf(name);
    }
}
