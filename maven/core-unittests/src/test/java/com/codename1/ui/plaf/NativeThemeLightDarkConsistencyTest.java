package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import com.codename1.ui.util.Resources;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the light and dark appearances of the shipped native themes against
 * drifting apart structurally. The recurring bug this pins down: a dark-mode
 * CSS override redeclares {@code border:} (usually just to recolor the
 * hairline) without repeating {@code border-radius} or the
 * {@code cn1-background-type}, so the compiler emits a plain rectangular
 * border for dark while light keeps its rounded/pill shape. That shipped
 * twice already - the dark ChatInput attach/voice buttons rendered as squares
 * instead of circles, and the dark MultiButton cell rendered as a square
 * instead of a rounded rect - and pixel gates only catch it when the golden
 * happens to be current.
 *
 * The theme hashtable stores dark styles under {@code $Dark<UIID>} keys
 * (see UIManager's dark-mode resolution), with the same {@code .border} /
 * {@code sel#border} / {@code press#border} / {@code dis#border} suffixes as
 * the light styles. Whenever BOTH appearances declare a border for the same
 * UIID+state, the two border objects must be the same shape class
 * (Border vs RoundBorder vs RoundRectBorder vs CSSBorder): recoloring is
 * appearance-specific, geometry is not. A dark key with no light counterpart
 * (or vice versa) is fine - the missing side inherits.
 *
 * Loads the `.res` straight from the repo's `Themes/` build output and
 * silently skips when absent (same convention as NativeThemeBindingsTest),
 * so the test only fires when a freshly-built native theme is on disk.
 */
public class NativeThemeLightDarkConsistencyTest extends UITestBase {

    @Test
    public void iosModernLightAndDarkBorderShapesMatch() throws Exception {
        assertLightDarkBorderShapeParity("iOSModernTheme.res");
    }

    @Test
    public void androidMaterialLightAndDarkBorderShapesMatch() throws Exception {
        assertLightDarkBorderShapeParity("AndroidMaterialTheme.res");
    }

    private void assertLightDarkBorderShapeParity(String fileName) throws Exception {
        File themeFile = locateNativeTheme(fileName);
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
        Hashtable theme = res.getTheme(res.getThemeResourceNames()[0]);

        List<String> mismatches = new ArrayList<String>();
        for (Object keyObj : new TreeSet<Object>(theme.keySet())) {
            String darkKey = keyObj.toString();
            if (!darkKey.startsWith("$Dark")) {
                continue;
            }
            if (!darkKey.endsWith(".border") && !darkKey.endsWith("#border")) {
                continue;
            }
            String lightKey = darkKey.substring("$Dark".length());
            Object dark = theme.get(darkKey);
            Object light = theme.get(lightKey);
            if (dark == null || light == null) {
                // Only one appearance declares a border for this state; the
                // other inherits it, so the shapes cannot diverge.
                continue;
            }
            if (!light.getClass().equals(dark.getClass())) {
                mismatches.add(lightKey + ": light=" + light.getClass().getSimpleName()
                        + " dark=" + dark.getClass().getSimpleName());
            }
        }
        assertTrue(mismatches.isEmpty(), fileName
                + " light/dark border shape mismatch (a dark override probably redeclared"
                + " 'border:' without repeating border-radius / cn1-background-type): "
                + mismatches);
    }

    /// Searches a few well-known relative locations for a freshly-built
    /// native theme `.res` (same lookup as NativeThemeBindingsTest): the
    /// surefire `user.dir` lands inside the unittests module while the
    /// build output lives at the repo root's `Themes/`.
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
