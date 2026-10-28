package github.naivg.offlineskininjector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTest {

    @Test
    void normalizesUuidsWithAndWithoutDashes() {
        String expected = "069a79f444e94726a5befca90e38aaf5";
        assertEquals(expected, Config.normalizeUuid("069a79f4-44e9-4726-a5be-fca90e38aaf5"));
        assertEquals(expected, Config.normalizeUuid("069A79F4-44E9-4726-A5BE-FCA90E38AAF5"));
        assertEquals(expected, Config.normalizeUuid("069a79f444e94726a5befca90e38aaf5"));
    }

    @Test
    void rejectsMalformedUuids() {
        assertNull(Config.normalizeUuid(null));
        assertNull(Config.normalizeUuid(""));
        assertNull(Config.normalizeUuid("not-a-uuid"));
        assertNull(Config.normalizeUuid("069a79f444e94726a5befca90e38aaf"));
        assertNull(Config.normalizeUuid("069a79f444e94726a5befca90e38aaf5z"));
    }

    @Test
    void modeDefaultsToSelf() {
        assertEquals(Config.Mode.SELF, Config.Mode.parse(null));
        assertEquals(Config.Mode.SELF, Config.Mode.parse("self"));
        assertEquals(Config.Mode.ALL, Config.Mode.parse("ALL"));
    }

    @Test
    void modelDefaultsToAuto() {
        assertEquals(SkinModel.AUTO, SkinModel.parse(null));
        assertEquals(SkinModel.AUTO, SkinModel.parse(""));
        assertEquals(SkinModel.AUTO, SkinModel.parse("AUTO"));
        assertEquals(SkinModel.CLASSIC, SkinModel.parse("classic"));
        assertEquals(SkinModel.SLIM, SkinModel.parse(" SLIM "));
    }

    @Test
    void rejectsUnknownModels() {
        assertThrows(IllegalArgumentException.class, () -> SkinModel.parse("wide"));
    }

    @Test
    void exposesTheMetadataValue() {
        assertEquals("classic", SkinModel.CLASSIC.metadata());
        assertEquals("slim", SkinModel.SLIM.metadata());
        assertThrows(IllegalStateException.class, SkinModel.AUTO::metadata);
    }

    @Test
    void formatDefaultsToAuto() {
        assertEquals(Config.Format.AUTO, Config.Format.parse(null));
        assertEquals(Config.Format.AUTO, Config.Format.parse("AUTO"));
        assertEquals(Config.Format.MODERN, Config.Format.parse("64x64"));
        assertEquals(Config.Format.MODERN, Config.Format.parse("modern"));
        assertEquals(Config.Format.LEGACY, Config.Format.parse("64x32"));
        assertEquals(Config.Format.LEGACY, Config.Format.parse("legacy"));
    }

    @Test
    void readsTheOptionalCape(@TempDir Path tempDir) throws Exception {
        Path skin = tempDir.resolve("skin.png");
        Path cape = tempDir.resolve("cape.png");
        Files.write(skin, new byte[]{1});
        Files.write(cape, new byte[]{2});

        String oldSkin = System.getProperty("offlineskininjector.skin");
        String oldCape = System.getProperty("offlineskininjector.cape");
        try {
            System.setProperty("offlineskininjector.skin", skin.toString());
            System.setProperty("offlineskininjector.cape", cape.toString());

            Config config = Config.load();
            assertEquals(cape.toFile(), config.capeFile());
        } finally {
            restore("offlineskininjector.skin", oldSkin);
            restore("offlineskininjector.cape", oldCape);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
