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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.ui.Display;
import com.codename1.ui.Painter;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

/// The half of a peer that is the same for every peer: it connects one
/// Codename One component to the AWT component it shows.
public final class PeerSupport {

    private final Component owner;
    private final com.codename1.ui.Component peer;
    private G2D painting;

    public PeerSupport(Component owner, com.codename1.ui.Component peer) {
        this.owner = owner;
        this.peer = peer;
        peer.setOpaque(false);
    }

    /// The AWT component.
    public Component owner() {
        return owner;
    }

    /// Runs the owner's painting with `g`, the context the peer's `paint`
    /// received. The graphics starts with the owner's foreground and font.
    public void paintOwner(com.codename1.ui.Graphics g) {
        G2D g2 = G2D.forPeer(g, peer.getX(), peer.getY(), peer.getWidth(), peer.getHeight());
        G2D saved = painting;
        painting = g2;
        try {
            Color fg = owner.getForeground();
            if (fg != null) {
                g2.setColor(fg);
            }
            Font f = owner.getFont();
            if (f != null) {
                g2.setFont(f);
            }
            owner.update(g2);
        } finally {
            painting = saved;
            g2.finish();
        }
    }

    /// Whether `g` draws to the context this peer is being painted with.
    public boolean isPainting(G2D g) {
        return painting != null && g.nativeGraphics() == painting.nativeGraphics();
    }

    /// Paints the peer's native look at the origin of `g`.
    public void paintNative(G2D g) {
        if (peer instanceof Peer && ((Peer) peer).nativeLook()) {
            g.paintNative(peer.getX(), peer.getY(), new NativePass((Peer) peer, false));
        }
    }

    /// Paints the peer's children at the origin of `g`.
    public void paintChildren(G2D g) {
        if (peer instanceof Peer) {
            g.paintNative(peer.getX(), peer.getY(), new NativePass((Peer) peer, true));
        }
    }

    /// One pass of a peer's own painting: its look, or its children.
    private static final class NativePass implements G2D.NativePainter {
        private final Peer target;
        private final boolean children;

        NativePass(Peer target, boolean children) {
            this.target = target;
            this.children = children;
        }

        @Override
        public void paint(com.codename1.ui.Graphics ng) {
            if (children) {
                target.paintNativeChildren(ng);
            } else {
                target.paintNativeLook(ng);
            }
        }
    }

    /// Paints the background the peer's style describes, which Codename
    /// One itself no longer paints because the peer is not opaque.
    public void paintStyleBackground(com.codename1.ui.Graphics g) {
        Style s = peer.getStyle();
        Border b = s.getBorder();
        if (b != null && b.isBackgroundPainter()) {
            b.paintBorderBackground(g, peer);
            return;
        }
        Painter p = s.getBgPainter();
        if (p != null) {
            p.paint(g, new Rectangle(peer.getX(), peer.getY(), peer.getWidth(), peer.getHeight()));
        }
    }

    /// Gives a canvas or container peer a style that draws and reserves
    /// nothing.
    public static void strip(com.codename1.ui.Component c) {
        c.setUIID("Container");
        Style s = c.getAllStyles();
        s.setBgTransparency(0);
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
        s.setBorder(null);
    }

    /// Pushes the foreground, background and font that were set on the
    /// owner into the styles of a peer with a native look.
    public void applyStyle() {
        if (!(peer instanceof Peer) || !((Peer) peer).nativeLook()) {
            return;
        }
        Style s = peer.getAllStyles();
        if (owner.isForegroundSet()) {
            s.setFgColor(owner.getForeground().getRGB() & 0xffffff);
        }
        if (owner.isBackgroundSet()) {
            Color bg = owner.getBackground();
            s.setBgColor(bg.getRGB() & 0xffffff);
            s.setBgTransparency(bg.getAlpha());
        }
        if (owner.isFontSet()) {
            Font f = owner.getFont();
            s.setFont(Fonts.nativeFont(f, f.getSize2D() * Units.scale()));
        }
    }

    /// Reports the peer's focus changes to the owner as focus events.
    public void trackFocus() {
        peer.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(com.codename1.ui.Component cmp) {
                EventBridge.setFocusOwner(owner, false);
            }

            @Override
            public void focusLost(com.codename1.ui.Component cmp) {
                EventBridge.nativeFocusLost(owner);
            }
        });
    }

    /// Sends a repaint the peer asked for through the root of the peer
    /// tree, because what lies behind a peer is painted by its ancestors.
    /// Answers `false` when the peer is the root and must repaint itself.
    public boolean routeRepaint() {
        if (owner.getParent() == null || owner.getParent().cn1PeerOrNull() == null) {
            return false;
        }
        repaint(owner, 0, 0, owner.getWidth(), owner.getHeight());
        return true;
    }

    private static Component root(Component c) {
        Component t = c;
        while (t.getParent() != null && t.getParent().cn1PeerOrNull() != null) {
            t = t.getParent();
        }
        return t;
    }

    /// Repaints a rectangle of `c`, given in its logical coordinates.
    public static void repaint(Component c, int x, int y, int width, int height) {
        com.codename1.ui.Component p = c.cn1PeerOrNull();
        if (p == null || p.getComponentForm() == null) {
            return;
        }
        com.codename1.ui.Component rp = root(c).cn1PeerOrNull();
        float s = Units.scale();
        int x1 = (int) Math.floor(x * s);
        int y1 = (int) Math.floor(y * s);
        int x2 = (int) Math.ceil((x + width) * s);
        int y2 = (int) Math.ceil((y + height) * s);
        rp.repaint(p.getAbsoluteX() + x1, p.getAbsoluteY() + y1, x2 - x1, y2 - y1);
    }

    /// Has the window `c` is in laid out again on the next turn of the
    /// event dispatch thread, and repainted.
    public static void scheduleLayout(Component c) {
        if (!Display.isInitialized()) {
            return;
        }
        com.codename1.ui.Component rp = root(c).cn1PeerOrNull();
        if (rp instanceof com.codename1.ui.Container && rp.getComponentForm() != null) {
            ((com.codename1.ui.Container) rp).revalidateLater();
        }
    }
}
