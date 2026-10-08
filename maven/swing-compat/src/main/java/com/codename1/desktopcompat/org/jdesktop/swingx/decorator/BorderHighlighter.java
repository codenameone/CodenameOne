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
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.javax.swing.border.CompoundBorder;

/// Puts a border on the cell's component: around the border the renderer
/// gave it, inside that border, or instead of it.
public class BorderHighlighter extends AbstractHighlighter {

    private Border paddingBorder;
    private boolean inner;
    private boolean compound;

    public BorderHighlighter() {
        this((HighlightPredicate) null, null);
    }

    public BorderHighlighter(HighlightPredicate predicate) {
        this(predicate, null);
    }

    public BorderHighlighter(Border paddingBorder) {
        this(null, paddingBorder);
    }

    public BorderHighlighter(HighlightPredicate predicate, Border paddingBorder) {
        this(predicate, paddingBorder, true);
    }

    public BorderHighlighter(HighlightPredicate predicate, Border paddingBorder, boolean compound) {
        this(predicate, paddingBorder, compound, false);
    }

    /// Makes a border highlighter. With `compound` the border is combined
    /// with the component's own, inside it when `inner` and around it
    /// otherwise; without, it replaces the component's border.
    public BorderHighlighter(HighlightPredicate predicate, Border paddingBorder, boolean compound, boolean inner) {
        super(predicate);
        this.paddingBorder = paddingBorder;
        this.compound = compound;
        this.inner = inner;
    }

    @Override
    protected Component doHighlight(Component renderer, ComponentAdapter adapter) {
        if (!(renderer instanceof JComponent)) {
            return renderer;
        }
        JComponent c = (JComponent) renderer;
        Border own = c.getBorder();
        if (compound && own != null) {
            c.setBorder(inner ? new CompoundBorder(own, paddingBorder) : new CompoundBorder(paddingBorder, own));
        } else {
            c.setBorder(paddingBorder);
        }
        return renderer;
    }

    @Override
    protected boolean canHighlight(Component component, ComponentAdapter adapter) {
        return paddingBorder != null && component instanceof JComponent;
    }

    public void setCompound(boolean compound) {
        if (this.compound == compound) {
            return;
        }
        this.compound = compound;
        fireStateChanged();
    }

    public boolean isCompound() {
        return compound;
    }

    public void setInner(boolean inner) {
        if (this.inner == inner) {
            return;
        }
        this.inner = inner;
        fireStateChanged();
    }

    public boolean isInner() {
        return inner;
    }

    public void setBorder(Border padding) {
        if (areEqual(padding, paddingBorder)) {
            return;
        }
        paddingBorder = padding;
        fireStateChanged();
    }

    public Border getBorder() {
        return paddingBorder;
    }
}
