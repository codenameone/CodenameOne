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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.ui.Graphics;

/// The peer of a label: a Codename One label.
///
/// It has the theme's font and colors and none of its spacing. A Swing
/// label is exactly as large as its text and icon -- programs rely on it
/// when they put an image in a scroll pane or line a label up with a
/// field -- so the padding and margin a theme gives its labels, which
/// are there to keep a finger's distance between widgets a Codename One
/// layout stacks, are taken off.
public class LabelPeer extends com.codename1.ui.Label implements Peer {

    private final PeerSupport support;

    public LabelPeer(Component owner) {
        support = new PeerSupport(owner, this);
        com.codename1.ui.plaf.Style s = getAllStyles();
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
    }

    @Override
    public PeerSupport support() {
        return support;
    }

    @Override
    public boolean nativeLook() {
        return true;
    }

    @Override
    public void paintNativeLook(Graphics g) {
        support.paintStyleBackground(g);
        super.paint(g);
        super.paintBorder(g);
    }

    @Override
    public void paintNativeChildren(Graphics g) {
    }

    @Override
    public void paint(Graphics g) {
        support.paintOwner(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
    }

    @Override
    public void repaint() {
        if (support == null || !support.routeRepaint()) {
            super.repaint();
        }
    }
}
