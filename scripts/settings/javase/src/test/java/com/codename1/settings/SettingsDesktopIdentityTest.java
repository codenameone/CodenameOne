package com.codename1.settings;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SettingsDesktopIdentityTest {
    @Test
    public void desktopIdentityUsesSettingsName() {
        assertEquals("Codename One Settings", CodenameOneSettingsStub.APP_DISPLAY_NAME);
        assertNotNull(CodenameOneSettingsStub.APP_VERSION);
        assertFalse(CodenameOneSettingsStub.APP_VERSION.isEmpty());
    }

    @Test
    public void desktopTextEditingUsesNativeCaretAndPersistentFocus() throws Exception {
        String desktop = Files.readString(Path.of("src/desktop/java/com/codename1/settings/CodenameOneSettingsStub.java"));
        assertTrue(desktop.contains("JavaSEPort.setUseNativeInput(true)"));
        assertFalse(desktop.contains("JavaSEPort.setUseNativeInput(false)"));
    }

    @Test
    public void nativeThemeResourceIsPackagedForModernControls() {
        URL nativeTheme = CodenameOneSettingsStub.class.getResource("/NativeTheme.res");
        assertNotNull(nativeTheme,
                "JavaSE Settings must package /NativeTheme.res so includeNativeBool can style native components.");
    }

    @Test
    public void bundledExtensionCatalogIsPackaged() {
        URL catalog = CodenameOneSettingsStub.class.getResource("/com/codename1/settings/extensions/CN1Libs.xml");
        assertNotNull(catalog, "Settings should ship the legacy cn1lib XML catalog for offline extension browsing.");
    }

    @Test
    public void outdatedExtensionsCarryExplicitCompatibilityMetadata() throws Exception {
        URL catalog = CodenameOneSettingsStub.class.getResource("/com/codename1/settings/extensions/CN1Libs.xml");
        assertNotNull(catalog);
        String xml = new String(catalog.openStream().readAllBytes(), StandardCharsets.UTF_8);
        int admob = xml.indexOf("<name>Admob Fullscreen Ads</name>");
        int end = xml.indexOf("</plugin>", admob);
        assertTrue(admob >= 0 && end > admob);
        String entry = xml.substring(admob, end);
        assertTrue(entry.contains("<status>outdated</status>"));
        assertTrue(entry.contains("<warning>"));
    }

    @Test
    public void macOsOwnsTheOnlyAboutCommand() throws Exception {
        String common = Files.readString(Path.of("../common/src/main/java/com/codename1/settings/CodenameOneSettings.java"));
        String desktop = Files.readString(Path.of("src/desktop/java/com/codename1/settings/CodenameOneSettingsStub.java"));
        assertFalse(common.contains("popupAction(menu, \"About\""));
        assertFalse(common.contains("menuCommand(\"About\""));
        assertTrue(desktop.contains("setAboutHandler"));
    }

    @Test
    public void packagedVersionIsPropagatedToAboutDialog() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        String common = Files.readString(Path.of("../common/src/main/java/com/codename1/settings/CodenameOneSettings.java"));
        String desktop = Files.readString(Path.of("src/desktop/java/com/codename1/settings/CodenameOneSettingsStub.java"));
        assertTrue(pom.contains("<Implementation-Version>${project.version}</Implementation-Version>"));
        assertTrue(desktop.contains("getImplementationVersion()"));
        assertTrue(desktop.contains("settings.version"));
        assertTrue(common.contains("prop(\"settings.version\", \"development\")"));
    }

    @Test
    public void centralPublicationMetadataIsInheritedByAllModules() throws Exception {
        String parent = Files.readString(Path.of("../pom.xml"));
        assertTrue(parent.contains("<licenses>"));
        assertTrue(parent.contains("<developers>"));
        assertTrue(parent.contains("<scm>"));
        assertTrue(parent.contains("<artifactId>central-publishing-maven-plugin</artifactId>"));
        assertTrue(parent.contains("<artifactId>maven-gpg-plugin</artifactId>"));
    }
}
