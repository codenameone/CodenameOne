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
package com.codename1.desktopcompat.javax.swing.plaf;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.javax.swing.border.Border;

/// A border that a look and feel installed: it draws as the border it
/// wraps.
public class BorderUIResource implements Border, UIResource {

    private final Border delegate;

    public BorderUIResource(Border delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("null border delegate argument");
        }
        this.delegate = delegate;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        delegate.paintBorder(c, g, x, y, width, height);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return delegate.getBorderInsets(c);
    }

    @Override
    public boolean isBorderOpaque() {
        return delegate.isBorderOpaque();
    }
}
