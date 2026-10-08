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
package com.codename1.desktopcompat.java.awt;

import com.codename1.compat.jdk.LinkOnly;

/// AWT's heavyweight `Scrollbar`, a scroll bar drawn by the desktop's own
/// toolkit. There is no such widget here, and `javax.swing.JScrollBar` is the component
/// to use.
///
/// The class exists for one reason: libraries written against Swing ask
/// `component instanceof Scrollbar` to tell the two families apart, and that
/// question needs the type to exist. It is [LinkOnly], so the build accepts
/// the reference from a bundled library and reports it from an application's
/// own code. Nothing can construct one, which keeps the answer to that
/// question `false` for every component there is.
@LinkOnly
public class Scrollbar extends Component {

    public Scrollbar() {
        throw new UnsupportedOperationException("java.awt.Scrollbar is not available; use javax.swing.JScrollBar");
    }
}
