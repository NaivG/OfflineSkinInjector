package github.naivg.offlineskininjector;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class SkinConverterTest {

    private static final int TRANSPARENT = 0x00000000;

    @Test
    void mirrorsRightLimbsIntoLeftLimbs() {
        BufferedImage source = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);

        // Right leg faces.
        fill(source, 4, 16, 4, 4, 0xFFFF0001);   // top
        fill(source, 8, 16, 4, 4, 0xFFFF0002);   // bottom
        fill(source, 0, 20, 4, 12, 0xFFFF0003);  // outer
        fill(source, 4, 20, 4, 12, 0xFFFF0004);  // front
        fill(source, 8, 20, 4, 12, 0xFFFF0005);  // inner
        fill(source, 12, 20, 4, 12, 0xFFFF0006); // back

        // Right arm faces.
        fill(source, 44, 16, 4, 4, 0xFFFF0007);  // top
        fill(source, 48, 16, 4, 4, 0xFFFF0008);  // bottom
        fill(source, 40, 20, 4, 12, 0xFFFF0009); // outer
        fill(source, 44, 20, 4, 12, 0xFFFF000A); // front
        fill(source, 48, 20, 4, 12, 0xFFFF000B); // inner
        fill(source, 52, 20, 4, 12, 0xFFFF000C); // back

        BufferedImage modern = SkinConverter.toModern(source);

        assertNotSame(source, modern);
        assertEquals(64, modern.getWidth());
        assertEquals(64, modern.getHeight());

        // Left leg faces.
        assertRect(modern, 20, 48, 4, 4, 0xFFFF0001);
        assertRect(modern, 24, 48, 4, 4, 0xFFFF0002);
        assertRect(modern, 24, 52, 4, 12, 0xFFFF0003);
        assertRect(modern, 20, 52, 4, 12, 0xFFFF0004);
        assertRect(modern, 16, 52, 4, 12, 0xFFFF0005);
        assertRect(modern, 28, 52, 4, 12, 0xFFFF0006);

        // Left arm faces.
        assertRect(modern, 36, 48, 4, 4, 0xFFFF0007);
        assertRect(modern, 40, 48, 4, 4, 0xFFFF0008);
        assertRect(modern, 40, 52, 4, 12, 0xFFFF0009);
        assertRect(modern, 36, 52, 4, 12, 0xFFFF000A);
        assertRect(modern, 32, 52, 4, 12, 0xFFFF000B);
        assertRect(modern, 44, 52, 4, 12, 0xFFFF000C);
    }

    @Test
    void mirrorsPixelsInsideEachFace() {
        BufferedImage source = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 4; x++) {
                source.setRGB(4 + x, 20 + y, 0xFF000000 | (x << 16) | (y << 8) | 0x42);
            }
        }

        BufferedImage modern = SkinConverter.toModern(source);

        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 4; x++) {
                assertEquals(source.getRGB(4 + (3 - x), 20 + y), modern.getRGB(20 + x, 52 + y));
            }
        }
    }

    @Test
    void copiesTheWholeLegacyTopHalf() {
        BufferedImage source = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        // The hat area stays transparent, so the vanilla "fully opaque hat" cleanup does not fire.
        fill(source, 0, 0, 32, 16, 0xFF102030);
        fill(source, 0, 16, 64, 16, 0xFF405060);
        for (int x = 32; x < 64; x++) {
            for (int y = 0; y < 16; y++) {
                source.setRGB(x, y, TRANSPARENT);
            }
        }

        BufferedImage modern = SkinConverter.toModern(source);

        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                assertEquals(source.getRGB(x, y), modern.getRGB(x, y));
            }
        }
    }

    @Test
    void cropsModernSkinToLegacyTopHalf() {
        BufferedImage source = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                source.setRGB(x, y, 0xFF000000 | (x << 8) | y);
            }
        }
        // Keep a transparent hat pixel so the legacy cleanup does not clear the hat.
        source.setRGB(32, 0, TRANSPARENT);

        BufferedImage legacy = SkinConverter.toLegacy(source);

        assertNotSame(source, legacy);
        assertEquals(64, legacy.getWidth());
        assertEquals(32, legacy.getHeight());
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                assertEquals(source.getRGB(x, y), legacy.getRGB(x, y));
            }
        }
    }

    @Test
    void returnsTheSourceWhenAlreadyInTheRequestedLayout() {
        BufferedImage modern = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        BufferedImage legacy = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);

        assertSame(modern, SkinConverter.toModern(modern));
        assertSame(legacy, SkinConverter.toLegacy(legacy));
    }

    @Test
    void removesFullyOpaqueHatLikeTheVanillaClient() {
        BufferedImage modernSource = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        fill(modernSource, 32, 0, 32, 32, 0xFFFFFFFF);

        BufferedImage modern = SkinConverter.toModern(modernSource);

        // Hat cleared, right arm restored, copied left arm forced opaque.
        assertEquals(0, modern.getRGB(32, 0) >>> 24);
        assertEquals(0, modern.getRGB(63, 15) >>> 24);
        assertEquals(255, modern.getRGB(40, 20) >>> 24);
        assertEquals(255, modern.getRGB(55, 31) >>> 24);
        assertEquals(255, modern.getRGB(36, 52) >>> 24);

        BufferedImage legacySource = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        fill(legacySource, 32, 0, 32, 32, 0xFFFFFFFF);
        BufferedImage legacy = SkinConverter.toLegacy(legacySource);
        assertEquals(0, legacy.getRGB(32, 0) >>> 24);
        assertEquals(255, legacy.getRGB(40, 20) >>> 24);
    }

    @Test
    void keepsHatWithTransparency() {
        BufferedImage source = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        fill(source, 32, 0, 32, 32, 0xFFFFFFFF);
        source.setRGB(32, 0, TRANSPARENT);

        BufferedImage modern = SkinConverter.toModern(source);
        assertEquals(255, modern.getRGB(33, 0) >>> 24);

        BufferedImage legacy = SkinConverter.toLegacy(source);
        assertEquals(255, legacy.getRGB(33, 0) >>> 24);
    }

    @Test
    void transparentPixelsDoNotBleedColorIntoForcedOpaqueAreas() {
        BufferedImage modernSource = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        modernSource.setRGB(0, 0, 0x00FF0000);
        assertEquals(0xFF000000, SkinConverter.toModern(modernSource).getRGB(0, 0));

        BufferedImage legacySource = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        legacySource.setRGB(0, 0, 0x00FF0000);
        assertEquals(0xFF000000, SkinConverter.toLegacy(legacySource).getRGB(0, 0));
    }

    @Test
    void encodesPngWithAlpha() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x80123456);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(SkinConverter.encodePng(image)));

        assertEquals(64, decoded.getWidth());
        assertEquals(64, decoded.getHeight());
        assertEquals(0x80123456, decoded.getRGB(0, 0));
    }

    private static void fill(BufferedImage image, int x, int y, int width, int height, int argb) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                image.setRGB(x + dx, y + dy, argb);
            }
        }
    }

    private static void assertRect(BufferedImage image, int x, int y, int width, int height, int argb) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                assertEquals(argb, image.getRGB(x + dx, y + dy),
                        "pixel " + (x + dx) + "," + (y + dy));
            }
        }
    }
}
