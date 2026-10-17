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

/// The peer of a push button: a Codename One button.
public class ButtonPeer extends com.codename1.ui.Button implements Peer {

    private final PeerSupport support;
    private boolean plain;

    public ButtonPeer(Component owner) {
        support = new PeerSupport(owner, this);
        support.trackFocus();
    }

    @Override
    public PeerSupport support() {
        return support;
    }

    @Override
    public boolean nativeLook() {
        return true;
    }

    /// Whether the button is drawn without the theme's shape, as an icon
    /// in a tool bar is; a press then shows as a wash.
    public void setPlain(boolean plain) {
        this.plain = plain;
    }

    @Override
    public void paintNativeLook(Graphics g) {
        support.paintStyleBackground(g);
        if (plain && getState() == STATE_PRESSED) {
            // Without the theme's shape nothing else shows the press.
            int alpha = g.getAlpha();
            int color = g.getColor();
            g.setColor(getUnselectedStyle().getFgColor());
            g.setAlpha(56);
            g.fillRect(getX(), getY(), getWidth(), getHeight());
            g.setAlpha(alpha);
            g.setColor(color);
        }
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
