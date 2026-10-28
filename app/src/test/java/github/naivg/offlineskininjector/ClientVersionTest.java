package github.naivg.offlineskininjector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientVersionTest {

    @Test
    void readsVersionFromLauncherCommandLine() {
        assertEquals("1.7.10", ClientVersion.fromCommand(
                "net.minecraft.client.main.Main --username Steve --version 1.7.10 --gameDir C:\\MC"));
        assertEquals("1.8.3-m3l_0.3b", ClientVersion.fromCommand(
                "Main --assetIndex 1.7.10 --version 1.8.3-m3l_0.3b"));
        assertEquals("1.7.10", ClientVersion.fromCommand(
                "Main --assetIndex 1.7.10 --username Steve"));
        assertEquals("1.7.10", ClientVersion.fromCommand(
                "Main --version=1.7.10 --username Steve"));
        assertEquals("1.7.10", ClientVersion.fromCommand(
                "Main --version \"1.7.10\" --username Steve"));
        assertNull(ClientVersion.fromCommand(null));
        assertNull(ClientVersion.fromCommand(""));
        assertNull(ClientVersion.fromCommand("Main --username Steve"));
    }

    @Test
    void readsVersionFromClasspath() {
        assertEquals("1.7.10", ClientVersion.fromClasspath(
                "C:\\MC\\versions\\1.7.10\\1.7.10.jar;C:\\MC\\libraries\\guava.jar"));
        assertEquals("1.8.3-m3l_0.3b", ClientVersion.fromClasspath(
                "/home/u/.minecraft/versions/1.8.3-m3l_0.3b/1.8.3-m3l_0.3b.jar"));
        assertNull(ClientVersion.fromClasspath("C:\\MC\\libraries\\guava.jar"));
        assertNull(ClientVersion.fromClasspath(null));
    }

    @Test
    void classifiesLegacyAndModernVersions() {
        assertTrue(ClientVersion.isLegacy("1.7.10"));
        assertTrue(ClientVersion.isLegacy("1.7.10-forge14.23.5.2847"));
        assertTrue(ClientVersion.isLegacy("1.6.2"));
        assertTrue(ClientVersion.isLegacy("1.5.2-rml_1.5.2"));
        assertTrue(ClientVersion.isLegacy("1.0"));
        assertTrue(ClientVersion.isLegacy("13w41b"));
        assertTrue(ClientVersion.isLegacy("1.7.10-pre1"));

        assertFalse(ClientVersion.isLegacy("1.8"));
        assertFalse(ClientVersion.isLegacy("1.8.3-m3l_0.3b"));
        assertFalse(ClientVersion.isLegacy("1.8-pre1"));
        assertFalse(ClientVersion.isLegacy("1.12.2-forge_14.23.5.2847"));
        assertFalse(ClientVersion.isLegacy("1.21.11"));
        assertFalse(ClientVersion.isLegacy("26.3"));
        assertFalse(ClientVersion.isLegacy("14w02a"));
        assertFalse(ClientVersion.isLegacy(null));
        assertFalse(ClientVersion.isLegacy("unknown"));
    }
}
