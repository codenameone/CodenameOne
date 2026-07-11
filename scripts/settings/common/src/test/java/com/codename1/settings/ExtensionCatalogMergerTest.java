package com.codename1.settings;

import com.codename1.settings.extensions.ExtensionCatalogMerger;
import com.codename1.settings.extensions.ExtensionDescriptor;
import com.codename1.settings.extensions.MavenDependency;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExtensionCatalogMergerTest {
    @Test
    public void bundledWarningSurvivesRemoteCatalogRefresh() {
        ExtensionDescriptor bundled = descriptor("outdated", "Older cn1lib warning");
        ExtensionDescriptor refreshed = descriptor("", "");

        List<ExtensionDescriptor> merged = ExtensionCatalogMerger.preserveCompatibilityMetadata(
                Arrays.asList(refreshed), Arrays.asList(bundled));

        assertEquals(1, merged.size());
        assertEquals("outdated", merged.get(0).status());
        assertEquals("Older cn1lib warning", merged.get(0).warning());
        assertTrue(merged.get(0).hasCompatibilityWarning());
        assertSame(refreshed.dependency(), merged.get(0).dependency());
    }

    @Test
    public void explicitRemoteWarningTakesPrecedence() {
        ExtensionDescriptor bundled = descriptor("outdated", "Bundled warning");
        ExtensionDescriptor refreshed = descriptor("unsupported", "Remote warning");

        ExtensionDescriptor merged = ExtensionCatalogMerger.preserveCompatibilityMetadata(
                Arrays.asList(refreshed), Arrays.asList(bundled)).get(0);

        assertSame(refreshed, merged);
        assertEquals("Remote warning", merged.warning());
    }

    private ExtensionDescriptor descriptor(String status, String warning) {
        return new ExtensionDescriptor("Admob Fullscreen Ads", "Remote description",
                new MavenDependency("com.codenameone", "admob-fullscreen-lib", "LATEST", "pom"), true,
                "AdmobFullScreen.cn1lib", "https://example.com", "Apache 2.0", "iOS, Android",
                "Author", "ads", "", "6", status, warning);
    }
}
