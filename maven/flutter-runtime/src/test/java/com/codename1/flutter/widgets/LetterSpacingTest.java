package com.codename1.flutter.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Flutter adds letterSpacing BETWEEN glyphs - n-1 gaps for n characters, with nothing
 * trailing the last one. Being one gap out is a whole space of drift on a short label,
 * which is exactly where the gallery uses it (headers and button captions).
 */
class LetterSpacingTest {

    @Test
    void spacingAddsOneGapFewerThanCharacters() {
        assertEquals(58.0, TextRenderElement.spacedWidth(50.0, 5, 2.0));
    }

    @Test
    void aSingleCharacterGetsNoSpacing() {
        assertEquals(10.0, TextRenderElement.spacedWidth(10.0, 1, 7.0));
    }

    @Test
    void zeroSpacingIsTheBareStringWidth() {
        assertEquals(50.0, TextRenderElement.spacedWidth(50.0, 5, 0.0));
    }

    @Test
    void theEmptyStringHasNoWidth() {
        assertEquals(0.0, TextRenderElement.spacedWidth(0.0, 0, 4.0));
    }

    @Test
    void negativeSpacingTightens() {
        assertEquals(42.0, TextRenderElement.spacedWidth(50.0, 5, -2.0));
    }
}
