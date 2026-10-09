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

import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.javax.swing.JComponent;

/// What the UI delegates of Swing have in common.
///
/// The layer draws its components itself and has no delegates of its
/// own. The class is here for the one place an application's delegate is
/// honoured: a `BasicTreeUI` set on a `JTree`, whose `paintRow` draws the
/// rows.
public abstract class ComponentUI {

    public ComponentUI() {
    }

    /// The delegate was set on `c`.
    public void installUI(JComponent c) {
    }

    /// The delegate was taken off `c`.
    public void uninstallUI(JComponent c) {
    }

    /// Not called: the component paints itself.
    public void paint(Graphics g, JComponent c) {
    }

    /// Not called: the component paints itself.
    public void update(Graphics g, JComponent c) {
        paint(g, c);
    }
}
