package github.naivg.offlineskininjector;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Converts between the 64x32 (1.7.x) and 64x64 (1.8+) skin layouts.
 *
 * <p>64x32 to 64x64 mirrors every face of the right arm and leg into the left limb slots, the
 * same way the vanilla client parses a legacy skin. 64x64 to 64x32 crops the top-left 64x32
 * area. Both directions apply the vanilla alpha cleanup.
 *
 * <p>Both methods return the source instance unchanged when it already has the requested
 * layout, so callers can keep the original PNG bytes.
 */
final class SkinConverter {

    static final int WIDTH = 64;
    static final int MODERN_HEIGHT = 64;
    static final int LEGACY_HEIGHT = 32;

    private SkinConverter() {
    }

    /** Returns a 64x64 skin. A 64x32 input gets its left limbs mirrored in. */
    static BufferedImage toModern(BufferedImage source) {
        checkSize(source);
        if (source.getHeight() == MODERN_HEIGHT) {
            return source;
        }

        BufferedImage out = new BufferedImage(WIDTH, MODERN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        // The whole legacy image is the top half of the modern image.
        copy(source, out, 0, 0, WIDTH, LEGACY_HEIGHT, 0, 0, false);

        // Left leg: mirror the six faces of the right leg.
        copyMirrored(source, out, 4, 16, 4, 4, 20, 48);   // top
        copyMirrored(source, out, 8, 16, 4, 4, 24, 48);   // bottom
        copyMirrored(source, out, 8, 20, 4, 12, 16, 52);  // inner (right side)
        copyMirrored(source, out, 4, 20, 4, 12, 20, 52);  // front
        copyMirrored(source, out, 0, 20, 4, 12, 24, 52);  // outer (left side)
        copyMirrored(source, out, 12, 20, 4, 12, 28, 52); // back

        // Left arm: mirror the six faces of the right arm.
        copyMirrored(source, out, 44, 16, 4, 4, 36, 48);  // top
        copyMirrored(source, out, 48, 16, 4, 4, 40, 48);  // bottom
        copyMirrored(source, out, 48, 20, 4, 12, 32, 52); // inner (right side)
        copyMirrored(source, out, 44, 20, 4, 12, 36, 52); // front
        copyMirrored(source, out, 40, 20, 4, 12, 40, 52); // outer (left side)
        copyMirrored(source, out, 52, 20, 4, 12, 44, 52); // back

        normalizeModern(out);
        return out;
    }

    /** Returns a 64x32 skin. A 64x64 input is cropped to its top-left 64x32. */
    static BufferedImage toLegacy(BufferedImage source) {
        checkSize(source);
        if (source.getHeight() == LEGACY_HEIGHT) {
            return source;
        }

        BufferedImage out = new BufferedImage(WIDTH, LEGACY_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        copy(source, out, 0, 0, WIDTH, LEGACY_HEIGHT, 0, 0, false);
        normalizeLegacy(out);
        return out;
    }

    static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("no PNG writer available");
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ pixels

    private static void copy(BufferedImage src, BufferedImage dst,
                             int sx, int sy, int w, int h, int dx, int dy, boolean mirror) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sourceX = mirror ? sx + (w - 1 - x) : sx + x;
                int argb = src.getRGB(sourceX, sy + y);
                // A fully transparent source pixel contributes nothing, so the destination
                // stays 0x00000000 instead of keeping hidden RGB values.
                dst.setRGB(dx + x, dy + y, (argb >>> 24) == 0 ? 0 : argb);
            }
        }
    }

    private static void copyMirrored(BufferedImage src, BufferedImage dst,
                                     int sx, int sy, int w, int h, int dx, int dy) {
        copy(src, dst, sx, sy, w, h, dx, dy, true);
    }

    // ------------------------------------------------------------------ vanilla alpha rules

    /**
     * Applies the 64x64 alpha rules: the head and right half of the body are opaque, fully
     * opaque overlay layers are removed, and the left limbs are forced opaque after mirroring.
     */
    private static void normalizeModern(BufferedImage image) {
        forceOpaque(image, 0, 0, 32, 16);          // head base
        clearIfFullyOpaque(image, 32, 0, 64, 32);  // hat + right arm; right arm is restored below
        forceOpaque(image, 0, 16, 64, 32);         // body + right arm + right leg
        clearIfFullyOpaque(image, 0, 32, 16, 48);  // right leg overlay
        clearIfFullyOpaque(image, 16, 32, 40, 48); // body overlay
        clearIfFullyOpaque(image, 40, 32, 56, 48); // right arm overlay
        clearIfFullyOpaque(image, 0, 48, 16, 64);  // left leg overlay
        forceOpaque(image, 16, 48, 48, 64);        // left leg + left arm base
        clearIfFullyOpaque(image, 48, 48, 64, 64); // left arm overlay
    }

    /** Applies the 64x32 alpha rules. */
    private static void normalizeLegacy(BufferedImage image) {
        forceOpaque(image, 0, 0, 32, 16);          // head base
        clearIfFullyOpaque(image, 32, 0, 64, 32);  // hat + right arm; right arm is restored below
        forceOpaque(image, 0, 16, 64, 32);         // body + right arm + right leg
    }

    private static void forceOpaque(BufferedImage image, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                image.setRGB(x, y, image.getRGB(x, y) | 0xFF000000);
            }
        }
    }

    private static void clearIfFullyOpaque(BufferedImage image, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if ((image.getRGB(x, y) >>> 24) < 128) {
                    return;
                }
            }
        }
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                image.setRGB(x, y, image.getRGB(x, y) & 0x00FFFFFF);
            }
        }
    }

    private static void checkSize(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("skin image is null");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width != WIDTH || (height != MODERN_HEIGHT && height != LEGACY_HEIGHT)) {
            throw new IllegalArgumentException(
                    "skin must be 64x64 or 64x32, got " + width + "x" + height);
        }
    }
}
