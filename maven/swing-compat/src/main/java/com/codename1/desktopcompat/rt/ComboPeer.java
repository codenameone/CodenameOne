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
import com.codename1.ui.list.ListModel;

/// The peer of a combo box: a Codename One combo box, which draws the
/// closed box and shows the popup the platform's way.
public class ComboPeer extends com.codename1.ui.ComboBox<Object> implements Peer {

    private final PeerSupport support;

    public ComboPeer(Component owner, ListModel<Object> model) {
        super(model);
        support = new PeerSupport(owner, this);
        support.trackFocus();
    }

    /// Opens the popup as a tap on the box would.
    public void openPopup() {
        if (isEnabled() && !isShowingPopupDialog()) {
            fireClicked();
        }
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
