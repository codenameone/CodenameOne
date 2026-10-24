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

/// Sets the background and the text color of a cell, with a second pair of
/// colors for selected cells. A `null` color leaves that property as the
/// renderer made it; a translucent one is mixed over what is there.
public class ColorHighlighter extends AbstractHighlighter {

    private Color background;
    private Color foreground;
    private Color selectedBackground;
    private Color selectedForeground;

    public ColorHighlighter() {
        this(null);
    }

    public ColorHighlighter(HighlightPredicate predicate) {
        this(predicate, null, null);
    }

    public ColorHighlighter(Color cellBackground, Color cellForeground) {
        this(null, cellBackground, cellForeground);
    }

    public ColorHighlighter(HighlightPredicate predicate, Color cellBackground, Color cellForeground) {
        this(predicate, cellBackground, cellForeground, null, null);
    }

    public ColorHighlighter(Color cellBackground, Color cellForeground, Color selectedBackground,
            Color selectedForeground) {
        this(null, cellBackground, cellForeground, selectedBackground, selectedForeground);
    }

    public ColorHighlighter(HighlightPredicate predicate, Color cellBackground, Color cellForeground,
            Color selectedBackground, Color selectedForeground) {
        super(predicate);
        this.background = cellBackground;
        this.foreground = cellForeground;
        this.selectedBackground = selectedBackground;
        this.selectedForeground = selectedForeground;
    }

    @Override
    protected Component doHighlight(Component renderer, ComponentAdapter adapter) {
        applyBackground(renderer, adapter);
        applyForeground(renderer, adapter);
        return renderer;
    }

    static Color cn1Blend(Color under, Color over) {
        if (over == null) {
            return under;
        }
        int a = over.getAlpha();
        if (a >= 255 || under == null) {
            return over;
        }
        float t = a / 255f;
        float u = 1 - t;
        return new Color(Math.round(under.getRed() * u + over.getRed() * t),
                Math.round(under.getGreen() * u + over.getGreen() * t),
                Math.round(under.getBlue() * u + over.getBlue() * t));
    }

    protected void applyBackground(Component renderer, ComponentAdapter adapter) {
        Color color = adapter.isSelected() ? getSelectedBackground() : getBackground();
        if (color != null) {
            renderer.setBackground(cn1Blend(renderer.getBackground(), color));
        }
    }

    protected void applyForeground(Component renderer, ComponentAdapter adapter) {
        Color color = adapter.isSelected() ? getSelectedForeground() : getForeground();
        if (color != null) {
            renderer.setForeground(cn1Blend(renderer.getForeground(), color));
        }
    }

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color color) {
        if (areEqual(color, background)) {
            return;
        }
        background = color;
        fireStateChanged();
    }

    public Color getForeground() {
        return foreground;
    }

    public void setForeground(Color color) {
        if (areEqual(color, foreground)) {
            return;
        }
        foreground = color;
        fireStateChanged();
    }

    public Color getSelectedBackground() {
        return selectedBackground;
    }

    public void setSelectedBackground(Color color) {
        if (areEqual(color, selectedBackground)) {
            return;
        }
        selectedBackground = color;
        fireStateChanged();
    }

    public Color getSelectedForeground() {
        return selectedForeground;
    }

    public void setSelectedForeground(Color color) {
        if (areEqual(color, selectedForeground)) {
            return;
        }
        selectedForeground = color;
        fireStateChanged();
    }
}
