package github.naivg.offlineskininjector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceTextureTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsModernAndLegacySkins() throws Exception {
        SourceTexture modern = SourceTexture.readSkin(png("skin64.png", 64, 64));
        assertEquals(64, modern.width());
        assertEquals(64, modern.height());
        assertTrue(modern.bytes().length > 0);

        SourceTexture legacy = SourceTexture.readSkin(png("skin32.png", 64, 32));
        assertEquals(64, legacy.width());
        assertEquals(32, legacy.height());
    }

    @Test
    void rejectsSkinsWithTheWrongSize() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> SourceTexture.readSkin(png("skin128.png", 128, 128)));
        assertThrows(IllegalArgumentException.class,
                () -> SourceTexture.readSkin(png("skin64x48.png", 64, 48)));
    }

    @Test
    void acceptsAClassicCape() throws Exception {
        SourceTexture cape = SourceTexture.readCape(png("cape.png", 64, 32));

        assertEquals(64, cape.width());
        assertEquals(32, cape.height());
        assertTrue(cape.bytes().length > 0);
    }

    @Test
    void rejectsACapeThatIsNot64x32() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> SourceTexture.readCape(png("cape64.png", 64, 64)));
        assertThrows(IllegalArgumentException.class,
                () -> SourceTexture.readCape(png("cape32.png", 32, 32)));
    }

    @Test
    void reportsUnreadableAndMissingFiles() throws Exception {
        Path notAnImage = tempDir.resolve("not-an-image.png");
        Files.write(notAnImage, "not a png".getBytes("UTF-8"));
        assertThrows(IllegalArgumentException.class,
                () -> SourceTexture.readCape(notAnImage.toFile()));

        File missing = tempDir.resolve("missing.png").toFile();
        assertThrows(IllegalArgumentException.class,
                () -> SourceTexture.readSkin(missing));
    }

    private File png(String name, int width, int height) throws IOException {
        File file = tempDir.resolve(name).toFile();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (!ImageIO.write(image, "png", file)) {
            throw new IOException("no PNG writer available");
        }
        return file;
    }
}
