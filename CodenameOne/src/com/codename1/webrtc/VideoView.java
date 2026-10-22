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
package com.codename1.webrtc;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

/// Draws a video track: the local camera's preview or the other side's
/// picture. A native view on a device, laid out like any component.
///
/// Call [#release()] when the view is discarded, so the track stops drawing
/// into it.
public final class VideoView extends Container {
    private Object peer;

    /// #### Parameters
    ///
    /// - `track`: a video track
    ///
    /// - `mirror`: whether to flip it left to right, which is what people
    ///   expect of their own front camera
    public VideoView(MediaTrack track, boolean mirror) {
        super(new BorderLayout());
        setUIID("VideoView");
        if (track != null && track.isVideo() && WebRtc.isSupported()) {
            peer = WebRtc.bridge().createVideoView(track.handle(), mirror);
            if (peer instanceof Component) {
                add(BorderLayout.CENTER, (Component) peer);
            }
        }
    }

    /// Whether the track is being drawn: false on a port that cannot draw
    /// it, where the view stays empty.
    public boolean isRendering() {
        return peer instanceof Component;
    }

    public void release() {
        if (peer != null) {
            Object was = peer;
            peer = null;
            removeAll();
            WebRtc.bridge().releaseVideoView(was);
        }
    }
}
