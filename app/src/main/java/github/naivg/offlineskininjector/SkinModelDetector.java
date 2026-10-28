package github.naivg.offlineskininjector;

import java.awt.image.BufferedImage;

/**
 * Detects whether a 64x64 skin uses classic (4-pixel) or slim (3-pixel) arms.
 *
 * <p>A slim skin leaves two-pixel strips unused at the edges of each arm block:
 *
 * <pre>
 *   (50,16) 2x4   right arm bottom face, right edge
 *   (54,20) 2x12  right arm back face, right edge
 *   (42,48) 2x4   left arm bottom face, right edge
 *   (46,52) 2x12  left arm back face, right edge
 * </pre>
 *
 * <p>A transparent pixel in any strip, or all four strips filled with solid black, means slim.
 * Only the base layer is inspected, and a 64x32 skin is always classic.
 */
final class SkinModelDetector {

    private static final int OPAQUE_ALPHA = 0xFF;
    private static final int BLACK = 0xFF000000;

    /** Pixels that are only part of the skin in the classic 4-pixel arm layout. */
    private static final int[][] CLASSIC_ONLY_STRIPS = {
            {50, 16, 2, 4},   // right arm bottom face, right edge
            {54, 20, 2, 12},  // right arm back face, right edge
            {42, 48, 2, 4},   // left arm bottom face, right edge
            {46, 52, 2, 12},  // left arm back face, right edge
    };

    private SkinModelDetector() {
    }

    /**
     * Returns {@link SkinModel#SLIM} when the skin uses the 3-pixel arm layout, otherwise
     * {@link SkinModel#CLASSIC}. Never throws; anything that is not a readable 64x64 skin is
     * treated as classic.
     */
    static SkinModel detect(BufferedImage skin) {
        if (skin == null
                || skin.getWidth() != SkinConverter.WIDTH
                || skin.getHeight() != SkinConverter.MODERN_HEIGHT) {
            return SkinModel.CLASSIC;
        }

        boolean allBlack = true;
        for (int[] strip : CLASSIC_ONLY_STRIPS) {
            if (hasTransparency(skin, strip)) {
                return SkinModel.SLIM;
            }
            allBlack &= isSolidBlack(skin, strip);
        }
        return allBlack ? SkinModel.SLIM : SkinModel.CLASSIC;
    }

    private static boolean hasTransparency(BufferedImage image, int[] rect) {
        int x = rect[0];
        int y = rect[1];
        int width = rect[2];
        int height = rect[3];
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                if ((image.getRGB(x + dx, y + dy) >>> 24) != OPAQUE_ALPHA) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSolidBlack(BufferedImage image, int[] rect) {
        int x = rect[0];
        int y = rect[1];
        int width = rect[2];
        int height = rect[3];
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                if (image.getRGB(x + dx, y + dy) != BLACK) {
                    return false;
                }
            }
        }
        return true;
    }
}
