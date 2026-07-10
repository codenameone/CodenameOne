package com.codename1.settings;

import com.codename1.settings.hints.BuildHintCatalog;
import com.codename1.settings.hints.BuildHintType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BuildHintCatalogTest {
    @Test
    public void parsesDeveloperGuideBuildHintTable() {
        String doc = """
                Before
                |===
                |Name\t|Description

                |android.debug
                |true/false defaults to true - indicates whether to include debug.

                |ios.plistInject
                |Injects raw XML into the plist.

                |windows.signing.timestampUrl
                |RFC 3161 timestamp server URL.

                |===
                After
                """;
        BuildHintCatalog catalog = BuildHintCatalog.fromAsciiDoc(doc);
        assertNotNull(catalog.get("android.debug"));
        assertEquals(BuildHintType.BOOLEAN, catalog.get("android.debug").type());
        assertEquals(BuildHintType.XML, catalog.get("ios.plistInject").type());
        assertEquals(BuildHintType.URL, catalog.get("windows.signing.timestampUrl").type());
    }

    @Test
    public void packagedDeveloperGuideCatalogProvidesKnownHintTypes() throws Exception {
        try (InputStream in = CodenameOneSettings.class.getResourceAsStream(
                "/com/codename1/settings/hints/Advanced-Topics-Under-The-Hood.asciidoc")) {
            assertNotNull(in, "The Settings jar should carry the developer-guide build hint table.");
            String doc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            BuildHintCatalog catalog = BuildHintCatalog.fromAsciiDoc(doc);
            assertEquals(BuildHintType.INTEGER, catalog.get("java.version").type());
            assertEquals(BuildHintType.VERSION, catalog.get("build.cn1Version").type());
            assertEquals(BuildHintType.VERSION, catalog.get("ios.bundleVersion").type());
            assertEquals(BuildHintType.INTEGER, catalog.get("android.targetSDKVersion").type());
            assertEquals(BuildHintType.BOOLEAN, catalog.get("android.useAndroidX").type());
        }
    }
}
