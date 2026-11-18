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
package com.codename1.ui.chart;

import com.codename1.ui.CN;

/// Colours a chart is painted with. The default palette is the Okabe-Ito set, chosen so
/// neighbouring series stay distinguishable for the common forms of colour blindness, and
/// a dark variant is picked automatically when the device is in dark mode.
public class ChartTheme {
    private static final int[] LIGHT = {
        0x0072B2, 0xE69F00, 0x009E73, 0xCC79A7, 0x56B4E9, 0xD55E00, 0xF0E442, 0x000000
    };
    private static final int[] DARK = {
        0x56B4E9, 0xE69F00, 0x2BC497, 0xE29BC4, 0x8FD0F5, 0xF07F3C, 0xF0E442, 0xDDDDDD
    };

    private int[] palette;
    private int textColor;
    private int secondaryTextColor;
    private int gridColor;
    private int axisColor;
    private int tooltipBackground;
    private int tooltipText;
    private Integer legendTextColor;
    private int positiveColor = 0x26A69A;
    private int negativeColor = 0xEF5350;

    /// The theme for the device's current appearance.
    public static ChartTheme createDefault() {
        Boolean dark = CN.isDarkMode();
        return new ChartTheme(dark != null && dark.booleanValue());
    }

    /// A theme matching a component style: light text means a dark background, whatever the
    /// device setting says, so an application with its own dark theme gets light labels.
    ///
    /// #### Parameters
    ///
    /// - `style`: the chart's style; its foreground colour becomes the label colour
    ///
    /// #### Returns
    ///
    /// the derived theme
    public static ChartTheme fromStyle(com.codename1.ui.plaf.Style style) {
        int fg = style.getFgColor();
        int r = (fg >> 16) & 0xff;
        int g = (fg >> 8) & 0xff;
        int b = fg & 0xff;
        boolean lightText = r * 299 + g * 587 + b * 114 > 128000;
        ChartTheme t = new ChartTheme(lightText);
        t.textColor = fg;
        return t;
    }

    /// Applies colours a theme sets on the chart's part UIIDs: the foreground of `ChartAxis`
    /// colours axis labels and lines, `ChartGrid` the grid lines, `ChartLegend` the legend text, and
    /// the background and foreground of `ChartTooltip` the tooltip. A UIID the theme leaves out
    /// keeps the palette's colour.
    ///
    /// #### Parameters
    ///
    /// - `ui`: the UI manager to read the styles from
    ///
    /// #### Returns
    ///
    /// this theme
    public ChartTheme applyUIIDs(com.codename1.ui.plaf.UIManager ui) {
        // an undefined UIID resolves to the default style, so only differences from it are applied
        com.codename1.ui.plaf.Style none = ui.getComponentStyle("ChartUndefinedPart");
        com.codename1.ui.plaf.Style axis = ui.getComponentStyle("ChartAxis");
        if (axis.getFgColor() != none.getFgColor()) {
            if (legendTextColor == null) {
                // the legend shares the secondary text colour; an axis colour must not reach it
                legendTextColor = Integer.valueOf(secondaryTextColor);
            }
            secondaryTextColor = axis.getFgColor();
            axisColor = axis.getFgColor();
        }
        com.codename1.ui.plaf.Style grid = ui.getComponentStyle("ChartGrid");
        if (grid.getFgColor() != none.getFgColor()) {
            gridColor = grid.getFgColor();
        }
        com.codename1.ui.plaf.Style legend = ui.getComponentStyle("ChartLegend");
        if (legend.getFgColor() != none.getFgColor()) {
            legendTextColor = Integer.valueOf(legend.getFgColor());
        }
        com.codename1.ui.plaf.Style tip = ui.getComponentStyle("ChartTooltip");
        if (tip.getBgColor() != none.getBgColor()) {
            tooltipBackground = tip.getBgColor();
        }
        if (tip.getFgColor() != none.getFgColor()) {
            tooltipText = tip.getFgColor();
        }
        return this;
    }

    /// The legend text colour; the secondary text colour unless `ChartLegend` or
    /// `setLegendTextColor` sets one.
    public int getLegendTextColor() {
        return legendTextColor != null ? legendTextColor.intValue() : secondaryTextColor;
    }

    /// Sets the legend text colour.
    ///
    /// #### Parameters
    ///
    /// - `c`: an RGB colour
    ///
    /// #### Returns
    ///
    /// this theme
    public ChartTheme setLegendTextColor(int c) {
        legendTextColor = Integer.valueOf(c);
        return this;
    }

    private final boolean dark;

    /// Creates the light or dark default theme.
    ///
    /// #### Parameters
    ///
    /// - `dark`: true for colours suited to a dark background
    public ChartTheme(boolean dark) {
        this.dark = dark;
        if (dark) {
            palette = copy(DARK);
            textColor = 0xE6E6E6;
            secondaryTextColor = 0xA0A0A0;
            gridColor = 0x3A3A3A;
            axisColor = 0x707070;
            tooltipBackground = 0xF2F2F2;
            tooltipText = 0x202020;
        } else {
            palette = copy(LIGHT);
            textColor = 0x333333;
            secondaryTextColor = 0x6B6B6B;
            gridColor = 0xE6E6E6;
            axisColor = 0x9E9E9E;
            tooltipBackground = 0x262626;
            tooltipText = 0xFFFFFF;
        }
    }

    private static int[] copy(int[] a) {
        int[] r = new int[a.length];
        System.arraycopy(a, 0, r, 0, a.length);
        return r;
    }

    /// True for a theme meant for a dark background.
    public boolean isDark() {
        return dark;
    }

    /// A tint of `rgb` for nested levels: lighter on a light theme, darker on a dark one.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the base colour
    ///
    /// - `amount`: 0 for the colour itself, towards 1 for the background
    ///
    /// #### Returns
    ///
    /// the tint
    public int tint(int rgb, float amount) {
        int target = dark ? 0x000000 : 0xffffff;
        amount = Math.max(0, Math.min(1, amount));
        int r = (int) (((rgb >> 16) & 0xff) * (1 - amount) + ((target >> 16) & 0xff) * amount);
        int g = (int) (((rgb >> 8) & 0xff) * (1 - amount) + ((target >> 8) & 0xff) * amount);
        int b = (int) ((rgb & 0xff) * (1 - amount) + (target & 0xff) * amount);
        return (r << 16) | (g << 8) | b;
    }

    /// The colour assigned to the series at `index`, cycling through the palette.
    public int getSeriesColor(int index) {
        return palette[Math.abs(index) % palette.length];
    }

    /// Replaces the series palette.
    public ChartTheme setPalette(int... colors) {
        if (colors != null && colors.length > 0) {
            palette = copy(colors);
        }
        return this;
    }

    /// Labels and titles.
    public int getTextColor() {
        return textColor;
    }

    /// Sets the label colour.
    public ChartTheme setTextColor(int c) {
        textColor = c;
        return this;
    }

    /// Axis tick labels and the legend.
    public int getSecondaryTextColor() {
        return secondaryTextColor;
    }

    /// Sets the tick label colour.
    public ChartTheme setSecondaryTextColor(int c) {
        secondaryTextColor = c;
        return this;
    }

    /// Grid lines.
    public int getGridColor() {
        return gridColor;
    }

    /// Sets the grid colour.
    public ChartTheme setGridColor(int c) {
        gridColor = c;
        return this;
    }

    /// Axis lines and tick marks.
    public int getAxisColor() {
        return axisColor;
    }

    /// Sets the axis line colour.
    public ChartTheme setAxisColor(int c) {
        axisColor = c;
        return this;
    }

    /// Tooltip fill.
    public int getTooltipBackground() {
        return tooltipBackground;
    }

    /// Sets the tooltip fill.
    public ChartTheme setTooltipBackground(int c) {
        tooltipBackground = c;
        return this;
    }

    /// Tooltip text.
    public int getTooltipText() {
        return tooltipText;
    }

    /// Sets the tooltip text colour.
    public ChartTheme setTooltipText(int c) {
        tooltipText = c;
        return this;
    }

    /// Rising candles, positive waterfall steps.
    public int getPositiveColor() {
        return positiveColor;
    }

    /// Sets the rising colour.
    public ChartTheme setPositiveColor(int c) {
        positiveColor = c;
        return this;
    }

    /// Falling candles, negative waterfall steps.
    public int getNegativeColor() {
        return negativeColor;
    }

    /// Sets the falling colour.
    public ChartTheme setNegativeColor(int c) {
        negativeColor = c;
        return this;
    }
}
