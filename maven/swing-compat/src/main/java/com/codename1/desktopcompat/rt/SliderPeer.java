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

/// The peer of a slider: an editable Codename One slider, dragged
/// natively.
///
/// A Codename One slider draws itself as its background, which a peer does
/// not have because it is not opaque; the look is therefore painted with
/// the peer opaque for the length of that call. The owner may confine the
/// look to a band of the peer, which leaves it room for the tick marks and
/// labels it paints itself.
public class SliderPeer extends com.codename1.ui.Slider implements Peer {

    private final PeerSupport support;
    private int bandOffset;
    private int bandSize = -1;

    public SliderPeer(Component owner) {
        this(owner, true);
    }

    protected SliderPeer(Component owner, boolean editable) {
        support = new PeerSupport(owner, this);
        setEditable(editable);
        if (editable) {
            support.trackFocus();
        }
    }

    /// Confines the native look to `size` device pixels across the
    /// slider's direction, starting `offset` pixels in. A negative size
    /// gives the look the whole peer.
    public void setBand(int offset, int size) {
        bandOffset = offset;
        bandSize = size;
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
        int w = getWidth();
        int h = getHeight();
        int dx = 0;
        int dy = 0;
        if (bandSize >= 0) {
            if (isVertical()) {
                dx = bandOffset;
                setWidth(Math.min(bandSize, Math.max(0, w - dx)));
            } else {
                dy = bandOffset;
                setHeight(Math.min(bandSize, Math.max(0, h - dy)));
            }
        }
        setOpaque(true);
        g.translate(dx, dy);
        try {
            super.paintComponentBackground(g);
            super.paint(g);
            super.paintBorder(g);
        } finally {
            g.translate(-dx, -dy);
            setOpaque(false);
            setWidth(w);
            setHeight(h);
        }
    }

    @Override
    public void paintNativeChildren(Graphics g) {
    }

    /// Nothing: the look is painted by `paintNativeLook`, when the owner
    /// asks for it.
    @Override
    public void paintComponentBackground(Graphics g) {
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
