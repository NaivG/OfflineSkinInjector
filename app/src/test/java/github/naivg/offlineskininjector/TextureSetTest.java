package github.naivg.offlineskininjector;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureSetTest {

    private static final String SKIN_URL = "http://127.0.0.1:1234/skin/abc.png";
    private static final String CAPE_URL = "http://127.0.0.1:1234/cape/def.png";

    @Test
    void buildsTheJsonWithACape() {
        TextureSet textures = new TextureSet(SKIN_URL, CAPE_URL, "slim");

        String expected = "{\"textures\":{"
                + "\"SKIN\":{\"url\":\"" + SKIN_URL + "\","
                + "\"metadata\":{\"model\":\"slim\"}},"
                + "\"CAPE\":{\"url\":\"" + CAPE_URL + "\"}}}";
        assertEquals(expected, textures.json());
    }

    @Test
    void buildsTheJsonWithoutACape() {
        TextureSet textures = new TextureSet(SKIN_URL, "classic");

        String expected = "{\"textures\":{"
                + "\"SKIN\":{\"url\":\"" + SKIN_URL + "\","
                + "\"metadata\":{\"model\":\"classic\"}}}}";
        assertEquals(expected, textures.json());
        assertFalse(textures.json().contains("CAPE"));
    }

    @Test
    void encodesThePayloadAsBase64OfTheJson() {
        TextureSet textures = new TextureSet(SKIN_URL, CAPE_URL, "classic");

        assertEquals(Base64.getEncoder().encodeToString(
                textures.json().getBytes(StandardCharsets.UTF_8)), textures.payload());

        String decoded = new String(Base64.getDecoder().decode(textures.payload()),
                StandardCharsets.UTF_8);
        assertEquals(textures.json(), decoded);
    }

    @Test
    void reportsWhetherACapeIsPresent() {
        assertTrue(new TextureSet(SKIN_URL, CAPE_URL, "classic").hasCape());
        assertFalse(new TextureSet(SKIN_URL, "classic").hasCape());
    }

    @Test
    void exposesTheUrlsAndTheModel() {
        TextureSet withCape = new TextureSet(SKIN_URL, CAPE_URL, "slim");
        assertEquals(SKIN_URL, withCape.skinUrl());
        assertEquals(CAPE_URL, withCape.capeUrl());
        assertEquals("slim", withCape.model());

        TextureSet withoutCape = new TextureSet(SKIN_URL, "classic");
        assertEquals(SKIN_URL, withoutCape.skinUrl());
        assertNull(withoutCape.capeUrl());
        assertEquals("classic", withoutCape.model());
    }
}
