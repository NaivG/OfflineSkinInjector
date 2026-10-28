package github.naivg.offlineskininjector;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * A PNG read from disk together with its decoded pixels.
 *
 * <p>The original bytes are kept so a texture whose layout already matches the client can be
 * served byte-for-byte instead of being re-encoded.
 *
 * <p>Only the sizes the vanilla client accepts are allowed: skins are 64x64 or 64x32, capes are
 * the classic 64x32 layout.
 */
final class SourceTexture {

    private static final int CAPE_WIDTH = 64;
    private static final int CAPE_HEIGHT = 32;

    private final byte[] bytes;
    private final BufferedImage image;

    private SourceTexture(byte[] bytes, BufferedImage image) {
        this.bytes = bytes;
        this.image = image;
    }

    /** Reads a skin, accepting both the modern 64x64 and the legacy 64x32 layout. */
    static SourceTexture readSkin(File file) throws IOException {
        return read(file, "skin", SkinConverter.WIDTH,
                new int[]{SkinConverter.MODERN_HEIGHT, SkinConverter.LEGACY_HEIGHT});
    }

    /** Reads a cape, which always uses the classic 64x32 layout. */
    static SourceTexture readCape(File file) throws IOException {
        return read(file, "cape", CAPE_WIDTH, new int[]{CAPE_HEIGHT});
    }

    private static SourceTexture read(File file, String kind, int width, int[] allowedHeights)
            throws IOException {
        if (!file.isFile()) {
            throw new IllegalArgumentException(kind + " file not found: " + file.getAbsolutePath());
        }
        byte[] bytes = readAllBytes(file);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IllegalArgumentException("not a readable image: " + file.getAbsolutePath());
        }
        boolean valid = image.getWidth() == width;
        if (valid) {
            valid = false;
            for (int height : allowedHeights) {
                if (image.getHeight() == height) {
                    valid = true;
                    break;
                }
            }
        }
        if (!valid) {
            throw new IllegalArgumentException(kind + " must be " + describe(width, allowedHeights)
                    + ", got " + image.getWidth() + "x" + image.getHeight() + ": "
                    + file.getAbsolutePath());
        }
        return new SourceTexture(bytes, image);
    }

    private static String describe(int width, int[] heights) {
        StringBuilder sb = new StringBuilder();
        for (int height : heights) {
            if (sb.length() > 0) {
                sb.append(" or ");
            }
            sb.append(width).append('x').append(height);
        }
        return sb.toString();
    }

    private static byte[] readAllBytes(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    byte[] bytes() {
        return bytes;
    }

    BufferedImage image() {
        return image;
    }

    int width() {
        return image.getWidth();
    }

    int height() {
        return image.getHeight();
    }
}
