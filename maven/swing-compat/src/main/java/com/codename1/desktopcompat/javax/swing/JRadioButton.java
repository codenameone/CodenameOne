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

import com.codename1.desktopcompat.rt.RadioButtonPeer;

/// A radio button, shown by a Codename One radio button; put several in a
/// `ButtonGroup` to make them exclusive.
public class JRadioButton extends JToggleButton {

    public JRadioButton() {
        this(null, null, false);
    }

    public JRadioButton(Icon icon) {
        this(null, icon, false);
    }

    public JRadioButton(Action a) {
        this();
        setAction(a);
    }

    public JRadioButton(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public JRadioButton(String text) {
        this(text, null, false);
    }

    public JRadioButton(String text, boolean selected) {
        this(text, null, selected);
    }

    public JRadioButton(String text, Icon icon) {
        this(text, icon, false);
    }

    public JRadioButton(String text, Icon icon, boolean selected) {
        super(text, icon, selected);
        setBorderPainted(false);
        setHorizontalAlignment(LEADING);
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new RadioButtonPeer(this);
    }
}
