package com.codename1.flutter.material;

import com.codename1.flutter.TextStyle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The AppBar title falls back to the text theme's titleLarge.
 *
 * <p>Flutter resolves it as {@code AppBar.titleTextStyle ??
 * AppBarTheme.titleTextStyle ?? textTheme.titleLarge}. The last link was
 * missing, so a bar whose theme names no title style -- most of them -- fell
 * through to whatever a bare Text picks: about 16 logical pixels against
 * titleLarge's 22, which rendered every title in the gallery at roughly seven
 * tenths of its size.</p>
 */
class AppBarTitleStyleTest {

    @Test
    void withoutAThemeStyleTheTitleTakesTitleLarge() {
        TextStyle chosen = AppBarRenderElement.chooseTitleStyle(null, new TextTheme());
        assertEquals(22.0, chosen.getFontSize(), 0.001);
    }

    @Test
    void aThemeStyleWins() {
        TextStyle themed = new TextStyle();
        themed.fontSize(31);
        TextStyle chosen = AppBarRenderElement.chooseTitleStyle(themed, new TextTheme());
        assertEquals(31.0, chosen.getFontSize(), 0.001);
    }

    @Test
    void noTextThemeAtAllLeavesTheTitleUnstyled() {
        assertNull(AppBarRenderElement.chooseTitleStyle(null, null));
    }
}
