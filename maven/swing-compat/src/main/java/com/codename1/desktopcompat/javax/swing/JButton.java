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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.rt.ButtonPeer;

/// A push button, shown by a Codename One button.
public class JButton extends AbstractButton implements Accessible {

    public JButton() {
        this(null, null);
    }

    public JButton(Icon icon) {
        this(null, icon);
    }

    public JButton(String text) {
        this(text, null);
    }

    public JButton(Action a) {
        this();
        setAction(a);
    }

    public JButton(String text, Icon icon) {
        setModel(new DefaultButtonModel());
        init(text, icon);
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new ButtonPeer(this);
    }

    public boolean isDefaultButton() {
        JRootPane root = SwingUtilities.getRootPane(this);
        return root != null && root.getDefaultButton() == this;
    }

    public boolean isDefaultCapable() {
        return true;
    }
}
