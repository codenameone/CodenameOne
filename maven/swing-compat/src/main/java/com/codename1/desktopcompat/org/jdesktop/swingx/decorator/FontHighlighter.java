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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Font;

/// Sets the font of the cell's component.
public class FontHighlighter extends AbstractHighlighter {

    private Font font;

    public FontHighlighter() {
        this((HighlightPredicate) null);
    }

    public FontHighlighter(Font font) {
        this(null, font);
    }

    public FontHighlighter(HighlightPredicate predicate) {
        this(predicate, null);
    }

    public FontHighlighter(HighlightPredicate predicate, Font font) {
        super(predicate);
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        if (areEqual(font, this.font)) {
            return;
        }
        this.font = font;
        fireStateChanged();
    }

    @Override
    protected boolean canHighlight(Component component, ComponentAdapter adapter) {
        return font != null;
    }

    @Override
    protected Component doHighlight(Component component, ComponentAdapter adapter) {
        component.setFont(font);
        return component;
    }
}
