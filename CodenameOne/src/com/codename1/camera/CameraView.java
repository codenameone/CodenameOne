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
package com.codename1.camera;

import com.codename1.ui.Container;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.layouts.BorderLayout;

/// Component that renders a live camera preview. Created via
/// `CameraSession#createView()` and added to a form like any other component.
///
/// ```java
/// CameraInfo info = Camera.getDefault(CameraFacing.BACK);
/// CameraSession session = Camera.open(info, new CameraSessionOptions());
/// CameraView view = session.createView();
/// view.setScaleType(ScaleType.CROP);
///
/// Form f = new Form("Camera", new BorderLayout());
/// f.add(BorderLayout.CENTER, view);
/// f.show();
///
/// session.setFrameListener(frame ->
///     BarcodeScanner.scan(frame.getJpegBytes()).ready(codes -> {
///         if (codes.length > 0) Log.p("scanned " + codes[0]);
///     }));
/// ```
///
/// The view holds a back-reference to its `CameraSession`; closing the session
/// while the view is still attached to a form leaves the view rendering its
/// last frame as a still image.
public final class CameraView extends Container {
    private final CameraSession session;
    private final PeerComponent peer;
    private boolean mirrored;
    private ScaleType scaleType = ScaleType.CROP;

    CameraView(CameraSession session, PeerComponent peer) {
        super(new BorderLayout());
        this.session = session;
        this.peer = peer;
        if (peer != null) {
            add(BorderLayout.CENTER, peer);
        }
    }

    public CameraSession getSession() {
        return session;
    }

    /// Whether to horizontally mirror the preview. Usually `true` for front
    /// cameras (matches the "mirror" feel users expect from a selfie view).
    public void setMirrored(boolean m) {
        this.mirrored = m;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public void setScaleType(ScaleType s) {
        this.scaleType = s == null ? ScaleType.CROP : s;
    }

    public ScaleType getScaleType() {
        return scaleType;
    }

    /// Exposed for ports that need to update the preview node directly.
    public PeerComponent getPreviewPeer() {
        return peer;
    }
}
