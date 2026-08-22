package com.codename1.settings;

import com.codename1.settings.hints.BuildHintCatalog;
import com.codename1.settings.hints.BuildHintMetadata;
import com.codename1.settings.hints.BuildHintType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hint catalog the Settings tool offers for editing.
 *
 * <p>It used to be scraped out of the developer guide's AsciiDoc table at runtime,
 * with each hint's type guessed by string-matching its description prose. It now
 * comes from {@code com.codename1.build.shared.BuildHints}, the same table the
 * build hint annotations are generated from and the same one the drift gate holds
 * the builders against.</p>
 */
public class BuildHintCatalogTest {

    @Test
    public void carriesTheHintsTheDeveloperGuideDocuments() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        assertNotNull(catalog.get("android.debug"));
        assertNotNull(catalog.get("ios.plistInject"));
        assertNotNull(catalog.get("windows.signing.timestampUrl"));
        assertTrue(catalog.all().size() > 400,
                "expected the full catalog, got " + catalog.all().size());
    }

    @Test
    public void knownHintsCarryTheRightType() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        assertEquals(BuildHintType.BOOLEAN, catalog.get("android.debug").type());
        assertEquals(BuildHintType.XML, catalog.get("ios.plistInject").type());
        assertEquals(BuildHintType.INTEGER, catalog.get("java.version").type());
        assertEquals(BuildHintType.INTEGER, catalog.get("android.min_sdk_version").type());
        assertEquals(BuildHintType.BOOLEAN, catalog.get("android.useAndroidX").type());
        assertEquals(BuildHintType.CSV, catalog.get("ios.pods").type());
    }

    /**
     * The tool used to accept any string for every hint but an integer, a version
     * or a URL. A hint with a closed domain is the one case where a wrong value is
     * certainly wrong, because the builder compares against those strings and
     * silently falls back to its default when it matches none of them.
     */
    @Test
    public void hintsWithAClosedDomainExposeIt() {
        BuildHintMetadata titleBar = BuildHintCatalog.load().get("desktop.titleBar");
        assertNotNull(titleBar);
        assertEquals(BuildHintType.ENUM, titleBar.type());
        assertTrue(titleBar.values().contains("native"));
        assertTrue(titleBar.values().contains("custom"));
        assertTrue(titleBar.values().contains("toolbar"));
        assertFalse(titleBar.values().contains("natvie"));
    }

    /** A hint with a checked form should say so, so the UI can point at it. */
    @Test
    public void annotatedHintsNameTheirAnnotation() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        assertEquals("@Ios(pods)", catalog.get("ios.pods").annotation());
        assertEquals("@Desktop(titleBar)", catalog.get("desktop.titleBar").annotation());
        // Not every hint has one; the properties file remains the way to set those.
        assertEquals(null, catalog.get("android.xmanifest").annotation());
    }

    /**
     * Dynamic families such as {@code android.permission.<NAME>} are patterns, not
     * keys, so there is nothing for the editor to set.
     */
    @Test
    public void dynamicFamiliesAreNotOffered() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        for (BuildHintMetadata h : catalog.all()) {
            assertFalse(h.name().contains("*"),
                    h.name() + " is a pattern, not a hint the editor can set");
        }
    }

    @Test
    public void searchStillMatchesOnNameAndDescription() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        assertFalse(catalog.search("pods").isEmpty());
        assertFalse(catalog.search("android").isEmpty());
    }
}
