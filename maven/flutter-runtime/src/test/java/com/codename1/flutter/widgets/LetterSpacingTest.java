/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.flutter.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /// The invariant the paint path has to hold: laying a run out glyph by glyph must
    /// end exactly where spacedWidth said it would.
    ///
    /// It did not. The paint path advanced by charWidth, which returns an INT, so every
    /// glyph's advance was rounded up and the error accumulated -- the same sentence
    /// measured 589px in the reference and drew 607px here, 3.1% wide, about 0.6px per
    /// character. Text that is systematically wide ellipsises strings that fit and
    /// clips the ones that do not, which is what Reply's sender lines did on iOS.
    private static double drawnWidth(double[] charWidths, double runWidth, double spacing) {
        double sum = 0;
        for (double w : charWidths) {
            sum += w;
        }
        double scale = TextRenderElement.trackingScale(runWidth, sum);
        double cursor = 0;
        for (int i = 0; i < charWidths.length - 1; i++) {
            cursor += charWidths[i] * scale + spacing;
        }
        return cursor + charWidths[charWidths.length - 1] * scale;
    }

    @Test
    void aTrackedRunEndsExactlyWhereItWasMeasured() {
        // charWidths rounded up from a true 12.4px advance, as an int-returning
        // charWidth does; the run itself measures 62, not 5 * 13 = 65.
        double[] widths = {13, 13, 13, 13, 13};
        double measured = TextRenderElement.spacedWidth(62.0, 5, 2.0);
        assertEquals(measured, drawnWidth(widths, 62.0, 2.0), 1e-9);
    }

    @Test
    void unevenGlyphsKeepTheirProportions() {
        double[] widths = {20, 5, 11, 4};
        double measured = TextRenderElement.spacedWidth(36.0, 4, 1.5);
        assertEquals(measured, drawnWidth(widths, 36.0, 1.5), 1e-9);
        // and the widest glyph is still the widest
        double scale = TextRenderElement.trackingScale(36.0, 40.0);
        assertEquals(0.9, scale, 1e-9);
    }

    /// Without the scale the run overruns, which is the defect stated numerically.
    @Test
    void theUnscaledRunOverrunsItsMeasurement() {
        double[] widths = {13, 13, 13, 13, 13};
        double measured = TextRenderElement.spacedWidth(62.0, 5, 2.0);
        double unscaled = 4 * (13 + 2.0) + 13;   // what the old paint path advanced
        assertTrue(unscaled > measured + 2,
                "expected the unscaled run to overrun; got " + unscaled + " vs " + measured);
    }

    /// A font that reports nothing must not divide by zero.
    @Test
    void aZeroWidthRunScalesByOne() {
        assertEquals(1.0, TextRenderElement.trackingScale(0.0, 0.0));
        assertEquals(1.0, TextRenderElement.trackingScale(10.0, 0.0));
    }
}
