/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ar;

import com.codename1.ui.Container;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.layouts.BorderLayout;

/// Component that renders the AR camera image composited with the session's
/// anchored 3D content. Created via `ARSession#createView()` and added to a
/// form like any other component.
///
/// To place content where the user taps, convert the pointer coordinate to
/// the normalized view coordinate expected by
/// `ARSession#hitTest(float, float)`:
///
/// ```java
/// view.addPointerReleasedListener(e -> {
///     float xn = (e.getX() - view.getAbsoluteX()) / (float) view.getWidth();
///     float yn = (e.getY() - view.getAbsoluteY()) / (float) view.getHeight();
///     session.hitTest(xn, yn).ready(hits -> { ... });
/// });
/// ```
public final class ARView extends Container {
    private final ARSession session;
    private final PeerComponent peer;

    ARView(ARSession session, PeerComponent peer) {
        super(new BorderLayout());
        this.session = session;
        this.peer = peer;
        if (peer != null) {
            add(BorderLayout.CENTER, peer);
        }
    }

    /// The `ARSession` backing this view.
    public ARSession getSession() {
        return session;
    }

    /// Exposed for ports that need to reach the native view directly.
    public PeerComponent getViewPeer() {
        return peer;
    }
}
