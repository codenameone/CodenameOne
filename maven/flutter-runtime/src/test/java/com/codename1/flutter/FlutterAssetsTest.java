package com.codename1.flutter;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The asset-flattening contract: Codename One resources are flat on every port
 * ({@code getResourceAsStream} rejects nested names), so Flutter asset paths
 * must encode into root-level resource names — unambiguously, and using only
 * characters that were already legal in the path.
 */
class FlutterAssetsTest {

    @Test
    void resourceNameIsFlatAndAbsolute() {
        String r = FlutterAssets.resourceName("assets/studies/reply_card.png");
        assertTrue(r.startsWith("/"), r);
        assertEquals(-1, r.indexOf('/', 1), "resource name must not be nested: " + r);
    }

    @Test
    void separatorAndEscape() {
        assertEquals("/cn1f_assets_studies_reply__card.png",
                FlutterAssets.resourceName("assets/studies/reply_card.png"));
    }

    @Test
    void packageAssetPath() {
        assertEquals("/cn1f_packages_gallery__assets_assets_icons_material_material.png",
                FlutterAssets.resourceName("packages/gallery_assets/assets/icons/material/material.png"));
    }

    @Test
    void leadingSlashesIgnored() {
        assertEquals(FlutterAssets.flatName("assets/a.png"), FlutterAssets.flatName("/assets/a.png"));
    }

    @Test
    void extensionSurvivesForNativeBundlers() {
        assertTrue(FlutterAssets.flatName("assets/fonts/Roboto_Bold.ttf").endsWith(".ttf"));
    }

    /** The doubling rule is what keeps '_' as a separator unambiguous. */
    @Test
    void underscoreVersusSeparatorDoNotCollide() {
        Set<String> seen = new HashSet<String>();
        String[] paths = {
            "a/b.png",      // separator
            "a_b.png",      // literal underscore at root
            "a/b/c.png",
            "a_b/c.png",
            "a/b_c.png",
        };
        for (String p : paths) {
            assertTrue(seen.add(FlutterAssets.flatName(p)),
                    "collision on " + p + " -> " + FlutterAssets.flatName(p));
        }
    }

    @Test
    void neverStartsWithReservedRawPrefix() {
        assertTrue(FlutterAssets.flatName("raw/thing.png").startsWith("cn1f_"));
    }
}
