package github.naivg.offlineskininjector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the class loader split: mod loaders append the agent jar to the system class path and
 * load their own copy of {@link Hooks}, so the state has to survive as system properties.
 */
class HooksTest {

    private static final String UUID = "380df991f603344ca090369bad2a924a";

    @AfterEach
    void clearProperties() {
        for (String key : new String[]{
                "offlineskininjector.skin",
                "offlineskininjector.cape",
                "offlineskininjector.mode",
                "offlineskininjector.model",
                "offlineskininjector.format",
                "offlineskininjector.uuid",
                "offlineskininjector.name",
                "offlineskininjector.runtime.skinUrl",
                "offlineskininjector.runtime.capeUrl",
                "offlineskininjector.runtime.model",
                "offlineskininjector.runtime.mode",
                "offlineskininjector.runtime.uuid",
                "offlineskininjector.runtime.name"}) {
            System.clearProperty(key);
        }
    }

    @Test
    void aSecondCopyRestoresTheStateFromProperties(@TempDir Path directory) throws Exception {
        Path skin = Files.createTempFile(directory, "skin", ".png");
        System.setProperty("offlineskininjector.skin", skin.toString());
        System.setProperty("offlineskininjector.uuid", UUID);
        Hooks.init(Config.load(),
                new TextureSet("http://127.0.0.1:1/skin.png", "http://127.0.0.1:1/cape.png", "slim"));

        // What the game class loader sees: the same class, but no init() call on that copy.
        clearStatics();

        assertEquals("http://127.0.0.1:1/skin.png", Hooks.skinUrl());
        assertTrue(Hooks.shouldOverride(new Profile(UUID, "Dev")));
    }

    @Test
    void aSecondCopyKeepsTheModeAndName(@TempDir Path directory) throws Exception {
        Path skin = Files.createTempFile(directory, "skin", ".png");
        System.setProperty("offlineskininjector.skin", skin.toString());
        System.setProperty("offlineskininjector.mode", "all");
        Hooks.init(Config.load(), new TextureSet("http://127.0.0.1:1/skin.png", "classic"));

        clearStatics();

        assertTrue(Hooks.shouldOverride(new Profile("00000000000000000000000000000000", "Other")));
    }

    private static void clearStatics() throws Exception {
        set("textures", null);
        set("overrideAll", false);
        set("localUuid", null);
        set("localName", null);
    }

    private static void set(String name, Object value) throws Exception {
        Field field = Hooks.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    public static final class Profile {
        private final String id;
        private final String name;

        Profile(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name.toUpperCase(Locale.ROOT); // name matching must ignore case
        }
    }
}
