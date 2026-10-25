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
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.IconAware;

/// Sets the icon of the cell's component, which has to be a label or
/// another component that takes an icon.
public class IconHighlighter extends AbstractHighlighter {

    private Icon icon;

    public IconHighlighter() {
        this((HighlightPredicate) null);
    }

    public IconHighlighter(HighlightPredicate predicate) {
        this(predicate, null);
    }

    public IconHighlighter(Icon icon) {
        this(null, icon);
    }

    public IconHighlighter(HighlightPredicate predicate, Icon icon) {
        super(predicate);
        this.icon = icon;
    }

    public void setIcon(Icon icon) {
        if (areEqual(icon, this.icon)) {
            return;
        }
        this.icon = icon;
        fireStateChanged();
    }

    public Icon getIcon() {
        return icon;
    }

    @Override
    protected Component doHighlight(Component component, ComponentAdapter adapter) {
        if (component instanceof IconAware) {
            ((IconAware) component).setIcon(icon);
        } else if (component instanceof JLabel) {
            ((JLabel) component).setIcon(icon);
        }
        return component;
    }

    @Override
    protected boolean canHighlight(Component component, ComponentAdapter adapter) {
        return icon != null && (component instanceof IconAware || component instanceof JLabel);
    }
}
