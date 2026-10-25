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
package com.codename1.desktopcompat.javax.swing.plaf.basic;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.javax.swing.AbstractButton;
import com.codename1.desktopcompat.javax.swing.JToolBar;
import com.codename1.desktopcompat.javax.swing.border.AbstractBorder;
import com.codename1.desktopcompat.javax.swing.plaf.UIResource;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;

/// The one border of the basic look and feel a program composes its own
/// borders with.
public class BasicBorders {

    /// An empty border as wide as the margin of the button, tool bar or
    /// text component it is set on, and zero on anything else.
    public static class MarginBorder extends AbstractBorder implements UIResource {

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            Insets margin = null;
            if (c instanceof AbstractButton) {
                margin = ((AbstractButton) c).getMargin();
            } else if (c instanceof JToolBar) {
                margin = ((JToolBar) c).getMargin();
            } else if (c instanceof JTextComponent) {
                margin = ((JTextComponent) c).getMargin();
            }
            insets.top = margin != null ? margin.top : 0;
            insets.left = margin != null ? margin.left : 0;
            insets.bottom = margin != null ? margin.bottom : 0;
            insets.right = margin != null ? margin.right : 0;
            return insets;
        }
    }
}
