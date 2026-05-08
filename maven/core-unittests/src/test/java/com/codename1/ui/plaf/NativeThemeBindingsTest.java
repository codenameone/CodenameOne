package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.util.Resources;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end check: load the shipped iOS Modern native theme `.res`,
 * verify the var() bindings the CSS compiler emitted survived the
 * round-trip, and that pushing an `@accent-color` override through
 * {@link UIManager#addThemeProps(Hashtable)} retunes a Button's
 * fgColor without touching `Button.fgColor` directly.
 *
 * Loads the `.res` straight from the repo's `Themes/` build output
 * (next to where `scripts/build-native-themes.sh` writes it). When
 * that file is absent the test is silently skipped rather than failed
 * - the runtime-side binding logic is already covered by
 * {@link UIManagerThemeBindingsTest}, so this test only fires when a
 * fresh native theme is sitting on disk.
 */
public class NativeThemeBindingsTest extends UITestBase {

    @Test
    public void iosModernThemeBindingRetunesButton() throws Exception {
        File themeFile = locateNativeTheme("iOSModernTheme.res");
        if (themeFile == null) {
            return;
        }
        Resources res;
        InputStream stream = new FileInputStream(themeFile);
        try {
            res = Resources.open(stream);
        } finally {
            stream.close();
        }
        String[] themeNames = res.getThemeResourceNames();
        assertNotNull(themeNames);
        Hashtable theme = res.getTheme(themeNames[0]);
        assertNotNull(theme);

        // Compiler should have emitted both the baked-in default AND
        // the binding entry. Resources.loadTheme stores colors as the
        // unpadded hex of their int value (Integer.toHexString), so the
        // expected default is "7aff" rather than "007aff".
        assertEquals("7aff", theme.get("Button.fgColor"));
        assertEquals("accent-color", theme.get("@cn1-bind:Button.fgColor"));

        UIManager.getInstance().setThemeProps(theme);

        Button defaultBtn = new Button("default");
        defaultBtn.setUIID("Button");
        assertEquals(0x007aff, defaultBtn.getUnselectedStyle().getFgColor(),
                "Native theme button should pick up the inlined fallback when no override is supplied");

        Hashtable override = new Hashtable();
        override.put("@accent-color", "ff2d95");
        UIManager.getInstance().addThemeProps(override);

        Button retuned = new Button("magenta");
        retuned.setUIID("Button");
        assertEquals(0xff2d95, retuned.getUnselectedStyle().getFgColor(),
                "@accent-color override should retune every UIID bound to --accent-color");
    }

    /// Searches a few well-known relative locations for a freshly-built
    /// native theme `.res`. We surfboard up a few directory levels
    /// because the surefire `user.dir` lands inside the unittests
    /// module while the build output lives at the repo root's
    /// `Themes/`.
    private static File locateNativeTheme(String fileName) {
        File cwd = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && cwd != null; i++) {
            File candidate = new File(cwd, "Themes/" + fileName);
            if (candidate.isFile()) {
                return candidate;
            }
            cwd = cwd.getParentFile();
        }
        return null;
    }
}
