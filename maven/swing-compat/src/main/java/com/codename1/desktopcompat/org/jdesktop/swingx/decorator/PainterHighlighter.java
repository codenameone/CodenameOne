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
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.Painter;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.PainterAware;

/// Gives the cell's component a [Painter] to draw its background with.
/// Only the renderer components of this subset's own renderers take one;
/// any other component is left alone.
///
/// A painter that changes is not observed: call `repaint()` on the table,
/// list or tree after changing one.
public class PainterHighlighter extends AbstractHighlighter {

    private Painter painter;

    public PainterHighlighter() {
        this((HighlightPredicate) null);
    }

    public PainterHighlighter(HighlightPredicate predicate) {
        this(predicate, null);
    }

    public PainterHighlighter(Painter painter) {
        this(null, painter);
    }

    public PainterHighlighter(HighlightPredicate predicate, Painter painter) {
        super(predicate);
        this.painter = painter;
    }

    public Painter getPainter() {
        return painter;
    }

    public void setPainter(Painter painter) {
        if (areEqual(painter, this.painter)) {
            return;
        }
        this.painter = painter;
        fireStateChanged();
    }

    @Override
    protected Component doHighlight(Component component, ComponentAdapter adapter) {
        if (component instanceof PainterAware) {
            ((PainterAware) component).setPainter(painter);
        }
        return component;
    }

    @Override
    protected boolean canHighlight(Component component, ComponentAdapter adapter) {
        return painter != null && component instanceof PainterAware;
    }
}
