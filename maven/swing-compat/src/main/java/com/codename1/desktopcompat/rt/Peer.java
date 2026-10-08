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

/// What every Codename One component that shows an AWT component
/// implements.
///
/// A peer never paints itself the way Codename One would: it is not
/// opaque, its `paint` hands over to [PeerSupport#paintOwner], which runs
/// the AWT component's `paint`, and its `paintBorder` does nothing. The
/// AWT side then calls back for the two things only the peer can draw.
public interface Peer {

    /// The support object made in the peer's constructor.
    PeerSupport support();

    /// Whether the peer is a Codename One widget with a look of its own
    /// (a button, a label), as opposed to a blank canvas or container.
    boolean nativeLook();

    /// Paints the widget as Codename One would: style background, content
    /// and border. Nothing for a peer without a native look.
    void paintNativeLook(com.codename1.ui.Graphics g);

    /// Paints the child peers; nothing for a peer that has none.
    void paintNativeChildren(com.codename1.ui.Graphics g);
}
