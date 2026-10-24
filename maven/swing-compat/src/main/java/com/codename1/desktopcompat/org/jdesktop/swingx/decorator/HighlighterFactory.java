/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.desktopcompat.org.jdesktop.swingx.decorator;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.rt.CellTheme;

/// Ready made striping highlighters.
///
/// The striping made without a color takes its color from the component:
/// the component's background with a little of its text color mixed in,
/// so it reads in a light theme and in a dark one.
public final class HighlighterFactory {

    public static final Color BEIGE = new Color(245, 245, 220);
    public static final Color LINE_PRINTER = new Color(0xCC, 0xCC, 0xFF);
    public static final Color CLASSIC_LINE_PRINTER = new Color(0xCC, 0xFF, 0xCC);
    public static final Color FLORAL_WHITE = new Color(255, 250, 240);
    public static final Color QUICKSILVER = new Color(0xF0, 0xF0, 0xE0);
    public static final Color GENERIC_GRAY = new Color(229, 229, 229);
    public static final Color LEDGER = new Color(0xF5, 0xFF, 0xF5);
    public static final Color NOTEPAD = new Color(0xFF, 0xFF, 0xCC);

    public HighlighterFactory() {
    }

    /// A stripe on every second row, in a color taken from the component.
    public static Highlighter createSimpleStriping() {
        return new ThemeStripe(HighlightPredicate.ODD);
    }

    /// Stripes of `rowsPerGroup` rows, in a color taken from the component.
    public static Highlighter createSimpleStriping(int rowsPerGroup) {
        return new ThemeStripe(new HighlightPredicate.RowGroupHighlightPredicate(rowsPerGroup));
    }

    /// A stripe of the given color on every second row.
    public static Highlighter createSimpleStriping(Color stripeBackground) {
        return new ColorHighlighter(HighlightPredicate.ODD, stripeBackground, null);
    }

    /// Stripes of `rowsPerGroup` rows in the given color.
    public static Highlighter createSimpleStriping(Color stripeBackground, int rowsPerGroup) {
        return new ColorHighlighter(new HighlightPredicate.RowGroupHighlightPredicate(rowsPerGroup),
                stripeBackground, null);
    }

    /// Rows alternating between white and a light gray.
    public static Highlighter createAlternateStriping() {
        return createAlternateStriping(Color.WHITE, GENERIC_GRAY);
    }

    /// Groups of `rowsPerGroup` rows alternating between white and a light
    /// gray.
    public static Highlighter createAlternateStriping(int rowsPerGroup) {
        return createAlternateStriping(Color.WHITE, GENERIC_GRAY, rowsPerGroup);
    }

    /// Rows alternating between two colors, the first row in
    /// `baseBackground`.
    public static Highlighter createAlternateStriping(Color baseBackground, Color alternateBackground) {
        ColorHighlighter base = new ColorHighlighter(HighlightPredicate.EVEN, baseBackground, null);
        ColorHighlighter alternate = new ColorHighlighter(HighlightPredicate.ODD, alternateBackground, null);
        return new CompoundHighlighter(base, alternate);
    }

    /// Groups of `linesPerStripe` rows alternating between two colors.
    public static Highlighter createAlternateStriping(Color baseBackground, Color alternateBackground,
            int linesPerStripe) {
        HighlightPredicate predicate = new HighlightPredicate.RowGroupHighlightPredicate(linesPerStripe);
        ColorHighlighter base = new ColorHighlighter(new HighlightPredicate.NotHighlightPredicate(predicate),
                baseBackground, null);
        ColorHighlighter alternate = new ColorHighlighter(predicate, alternateBackground, null);
        return new CompoundHighlighter(base, alternate);
    }

    /// A stripe whose color follows the component's own colors.
    private static final class ThemeStripe extends ColorHighlighter {

        ThemeStripe(HighlightPredicate predicate) {
            super(predicate);
        }

        @Override
        protected void applyBackground(Component renderer, ComponentAdapter adapter) {
            if (adapter.isSelected()) {
                return;
            }
            Color bg = adapter.getComponent().getBackground();
            Color fg = adapter.getComponent().getForeground();
            if (bg != null && fg != null) {
                renderer.setBackground(CellTheme.mix(bg, fg, 0.07f));
            }
        }
    }
}
