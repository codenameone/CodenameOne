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
package com.codename1.desktopcompat.org.jdesktop.swingx.renderer;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.border.Border;

/// Resets the visual properties of a renderer component for a cell: font,
/// enabled state, colors and border, to what the cell's context says.
///
/// A property is written only when it differs from what the component
/// has, because every write reaches the style of a Codename One widget.
public class DefaultVisuals<T extends JComponent> {

    private Color unselectedForeground;
    private Color unselectedBackground;

    public DefaultVisuals() {
    }

    /// Fixes the text color of unselected cells; `null` goes back to the
    /// context's.
    public void setForeground(Color c) {
        unselectedForeground = c;
    }

    /// Fixes the background of unselected cells; `null` goes back to the
    /// context's.
    public void setBackground(Color c) {
        unselectedBackground = c;
    }

    public void configureVisuals(T renderingComponent, CellContext context) {
        configurePainter(renderingComponent, context);
        configureState(renderingComponent, context);
        configureColors(renderingComponent, context);
        configureBorder(renderingComponent, context);
    }

    protected void configurePainter(T renderingComponent, CellContext context) {
        if (renderingComponent instanceof PainterAware && ((PainterAware) renderingComponent).getPainter() != null) {
            ((PainterAware) renderingComponent).setPainter(null);
        }
    }

    private static boolean cn1Same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    protected void configureState(T renderingComponent, CellContext context) {
        String name = context.getCellRendererName();
        if (!cn1Same(name, renderingComponent.getName())) {
            renderingComponent.setName(name);
        }
        if (renderingComponent.getToolTipText() != null) {
            renderingComponent.setToolTipText(null);
        }
        Font f = context.getFont();
        if (!cn1Same(f, renderingComponent.isFontSet() ? renderingComponent.getFont() : null)) {
            renderingComponent.setFont(f);
        }
        boolean enabled = context.getComponent() == null || context.getComponent().isEnabled();
        if (renderingComponent.isEnabled() != enabled) {
            renderingComponent.setEnabled(enabled);
        }
    }

    /// Does nothing: a renderer component of this layer has no preferred
    /// size of its own to reset.
    protected void configureSizes(T renderingComponent, CellContext context) {
    }

    protected void configureColors(T renderingComponent, CellContext context) {
        Color fg;
        Color bg;
        if (context.isSelected()) {
            fg = context.getSelectionForeground();
            bg = context.getSelectionBackground();
        } else {
            fg = getForeground(context);
            bg = getBackground(context);
        }
        if (!cn1Same(fg, renderingComponent.isForegroundSet() ? renderingComponent.getForeground() : null)) {
            renderingComponent.setForeground(fg);
        }
        if (!cn1Same(bg, renderingComponent.isBackgroundSet() ? renderingComponent.getBackground() : null)) {
            renderingComponent.setBackground(bg);
        }
        if (context.isFocused()) {
            configureFocusColors(renderingComponent, context);
        }
    }

    protected void configureFocusColors(T renderingComponent, CellContext context) {
        if (!context.isSelected() && context.isEditable()) {
            Color col = context.getFocusForeground();
            if (col != null) {
                renderingComponent.setForeground(col);
            }
            col = context.getFocusBackground();
            if (col != null) {
                renderingComponent.setBackground(col);
            }
        }
    }

    protected void configureBorder(T renderingComponent, CellContext context) {
        Border b = context.getBorder();
        if (renderingComponent.getBorder() != b) {
            renderingComponent.setBorder(b);
        }
    }

    protected Color getForeground(CellContext context) {
        return unselectedForeground != null ? unselectedForeground : context.getForeground();
    }

    protected Color getBackground(CellContext context) {
        return unselectedBackground != null ? unselectedBackground : context.getBackground();
    }
}
