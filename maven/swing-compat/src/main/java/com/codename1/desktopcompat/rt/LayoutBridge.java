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
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

/// The Codename One layout of a container peer: it runs the AWT layout,
/// which places the child peers as it sets the children's bounds.
///
/// Only the root of a peer tree takes its size from Codename One; below
/// it the AWT bounds are the truth and the peers follow.
public final class LayoutBridge extends Layout {

    private final Container owner;

    public LayoutBridge(Container owner) {
        this.owner = owner;
    }

    @Override
    public void layoutContainer(com.codename1.ui.Container parent) {
        if (!(parent.getParent() instanceof ContainerPeer)) {
            owner.cn1PeerResized(Units.toLogical(parent.getWidth()), Units.toLogical(parent.getHeight()));
        }
        owner.validate();
    }

    @Override
    public Dimension getPreferredSize(com.codename1.ui.Container parent) {
        com.codename1.desktopcompat.java.awt.Dimension d = owner.getPreferredSize();
        float s = Units.scale();
        return new Dimension((int) Math.ceil(d.width * s), (int) Math.ceil(d.height * s));
    }

    @Override
    public boolean isOverlapSupported() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LayoutBridge && ((LayoutBridge) o).owner == owner;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(owner);
    }
}
