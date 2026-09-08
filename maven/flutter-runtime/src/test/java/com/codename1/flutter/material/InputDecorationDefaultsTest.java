package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.EdgeInsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A decoration resolves against the ambient inputDecorationTheme.
 *
 * <p>Flutter's {@code InputDecoration.applyDefaults}: every field the widget
 * leaves unset falls back to the theme. The theme was held opaquely and never
 * read, so Rally -- which names its dark fill once on the theme rather than on
 * each of its login fields -- rendered white blocks on a dark page.</p>
 */
class InputDecorationDefaultsTest {

    private static InputDecorationThemeData darkFilledTheme() {
        InputDecorationThemeData t = new InputDecorationThemeData();
        t.filled(true);
        t.fillColor(new Color(0xFF33333DL));
        t.contentPadding(EdgeInsets.all(20));
        return t;
    }

    @Test
    void aThemeFillReachesADecorationThatNamesNone() {
        InputDecoration d = new InputDecoration();
        assertTrue(TextFieldRenderElement.resolveFilled(d, darkFilledTheme()));
        assertEquals(0xFF33333DL,
                TextFieldRenderElement.resolveFill(d, darkFilledTheme()).value());
    }

    @Test
    void theDecorationsOwnFillWins() {
        InputDecoration d = new InputDecoration();
        d.filled(true);
        d.fillColor(new Color(0xFFAABBCCL));
        assertEquals(0xFFAABBCCL,
                TextFieldRenderElement.resolveFill(d, darkFilledTheme()).value());
    }

    @Test
    void withNoThemeNothingIsFilled() {
        InputDecoration d = new InputDecoration();
        assertFalse(TextFieldRenderElement.resolveFilled(d, null));
        assertNull(TextFieldRenderElement.resolveFill(d, null));
        assertNull(TextFieldRenderElement.resolvePadding(d, null));
    }

    @Test
    void paddingFallsBackToTheThemeToo() {
        InputDecoration d = new InputDecoration();
        EdgeInsets got = (EdgeInsets) TextFieldRenderElement.resolvePadding(d, darkFilledTheme());
        assertEquals(20.0, got.left(), 0.001);
        d.contentPadding(EdgeInsets.all(4));
        got = (EdgeInsets) TextFieldRenderElement.resolvePadding(d, darkFilledTheme());
        assertEquals(4.0, got.left(), 0.001);
    }

    @Test
    void aThemeDataOnTheThemeIsReadableBack() {
        ThemeData theme = new ThemeData();
        InputDecorationThemeData t = darkFilledTheme();
        theme.inputDecorationTheme(t);
        assertEquals(t, theme.inputDecorationTheme());
        // Anything else stays opaque rather than being mistaken for one.
        theme.inputDecorationTheme("not a theme");
        assertNull(theme.inputDecorationTheme());
    }
}
