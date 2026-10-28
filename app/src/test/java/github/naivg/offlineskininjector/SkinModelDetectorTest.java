package github.naivg.offlineskininjector;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkinModelDetectorTest {

    private static final int TRANSPARENT = 0x00000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    /** The four strips that only contain pixels in the classic 4-pixel arm layout. */
    private static final int[][] CLASSIC_ONLY_STRIPS = {
            {50, 16, 2, 4},
            {54, 20, 2, 12},
            {42, 48, 2, 4},
            {46, 52, 2, 12},
    };

    @Test
    void detectsAClassicSkinFromItsFourPixelArms() {
        BufferedImage skin = opaqueSkin();

        assertEquals(SkinModel.CLASSIC, SkinModelDetector.detect(skin));
    }

    @Test
    void detectsASlimSkinWhenTheUnusedStripsAreTransparent() {
        BufferedImage skin = opaqueSkin();
        for (int[] strip : CLASSIC_ONLY_STRIPS) {
            fill(skin, strip, TRANSPARENT);
        }

        assertEquals(SkinModel.SLIM, SkinModelDetector.detect(skin));
    }

    @Test
    void oneTransparentPixelInAClassicOnlyStripIsEnough() {
        BufferedImage skin = opaqueSkin();
        skin.setRGB(54, 20, TRANSPARENT);

        assertEquals(SkinModel.SLIM, SkinModelDetector.detect(skin));
    }

    @Test
    void partiallyTransparentPixelsCountAsSlimEvidence() {
        BufferedImage skin = opaqueSkin();
        skin.setRGB(46, 52, 0x80FFFFFF);

        assertEquals(SkinModel.SLIM, SkinModelDetector.detect(skin));
    }

    @Test
    void allBlackStripsMeanSlimToo() {
        BufferedImage skin = opaqueSkin();
        for (int[] strip : CLASSIC_ONLY_STRIPS) {
            fill(skin, strip, BLACK);
        }

        assertEquals(SkinModel.SLIM, SkinModelDetector.detect(skin));
    }

    @Test
    void aSingleBlackStripIsNotSlimEvidence() {
        BufferedImage skin = opaqueSkin();
        fill(skin, CLASSIC_ONLY_STRIPS[0], BLACK);

        assertEquals(SkinModel.CLASSIC, SkinModelDetector.detect(skin));
    }

    @Test
    void detectsSlimFromEitherArm() {
        BufferedImage skin = opaqueSkin();
        fill(skin, CLASSIC_ONLY_STRIPS[2], TRANSPARENT);
        fill(skin, CLASSIC_ONLY_STRIPS[3], TRANSPARENT);

        assertEquals(SkinModel.SLIM, SkinModelDetector.detect(skin));
    }

    @Test
    void ignoresTransparencyOutsideTheArmStrips() {
        BufferedImage skin = opaqueSkin();
        fill(skin, 40, 32, 16, 16, TRANSPARENT); // right sleeve overlay
        fill(skin, 48, 48, 16, 16, TRANSPARENT); // left sleeve overlay
        fill(skin, 32, 0, 32, 16, TRANSPARENT);  // hat overlay

        assertEquals(SkinModel.CLASSIC, SkinModelDetector.detect(skin));
    }

    @Test
    void aLegacySkinIsAlwaysClassic() {
        BufferedImage skin = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        fill(skin, 0, 0, 64, 32, WHITE);
        fill(skin, 50, 16, 2, 4, TRANSPARENT);
        fill(skin, 54, 20, 2, 12, TRANSPARENT);

        assertEquals(SkinModel.CLASSIC, SkinModelDetector.detect(skin));
    }

    @Test
    void unsupportedImagesFallBackToClassic() {
        assertEquals(SkinModel.CLASSIC, SkinModelDetector.detect(null));
        assertEquals(SkinModel.CLASSIC, SkinModelDetector.detect(
                new BufferedImage(64, 48, BufferedImage.TYPE_INT_ARGB)));
        assertEquals(SkinModel.CLASSIC, SkinModelDetector.detect(
                new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)));
    }

    private static BufferedImage opaqueSkin() {
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        fill(skin, 0, 0, 64, 64, WHITE);
        return skin;
    }

    private static void fill(BufferedImage image, int[] rect, int argb) {
        fill(image, rect[0], rect[1], rect[2], rect[3], argb);
    }

    private static void fill(BufferedImage image, int x, int y, int width, int height, int argb) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                image.setRGB(x + dx, y + dy, argb);
            }
        }
    }
}
