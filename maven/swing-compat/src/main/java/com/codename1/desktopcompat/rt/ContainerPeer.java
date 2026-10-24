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

import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.ui.Graphics;

/// The peer of a container: its children are the peers of the container's
/// children, placed by a [LayoutBridge].
public class ContainerPeer extends com.codename1.ui.Container implements Peer {

    private final PeerSupport support;

    public ContainerPeer(Container owner) {
        super(new LayoutBridge(owner));
        PeerSupport.strip(this);
        setScrollableX(false);
        setScrollableY(false);
        support = new PeerSupport(owner, this);
    }

    @Override
    public PeerSupport support() {
        return support;
    }

    @Override
    public boolean nativeLook() {
        return false;
    }

    /// The peers are held in the order they are painted, which is the
    /// reverse of the order of the container's children: the first child
    /// of a Swing container is the one on top. The order of traversal is
    /// the children's, so the indices are given from the last peer to the
    /// first. A port asks the form for the component after the text field
    /// it is editing when Tab is pressed there, and the answer was the
    /// field before it -- or nothing, from the first field of a window.
    @Override
    public int updateTabIndices(int offset) {
        int idx = offset;
        for (int i = getComponentCount() - 1; i >= 0; i--) {
            com.codename1.ui.Component c = getComponentAt(i);
            int preferred = c.getPreferredTabIndex();
            if (preferred == 0) {
                c.setTabIndex(idx++);
            } else {
                c.setTabIndex(preferred);
            }
            if (c instanceof com.codename1.ui.Container) {
                idx = ((com.codename1.ui.Container) c).updateTabIndices(idx);
            }
        }
        return idx;
    }

    @Override
    public void paintNativeLook(Graphics g) {
    }

    @Override
    public void paintNativeChildren(Graphics g) {
        super.paint(g);
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
