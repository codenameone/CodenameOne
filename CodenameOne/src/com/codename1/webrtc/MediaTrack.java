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

/// One stream of audio or video: the local camera or microphone, or what
/// arrives from the other side.
///
/// A muted track keeps flowing as silence or black frames, so unmuting is
/// instant and the other side's connection never notices.
public final class MediaTrack {
    /// [#getKind()] of a microphone or a remote voice.
    public static final String AUDIO = "audio";

    /// [#getKind()] of a camera or a remote picture.
    public static final String VIDEO = "video";

    private final int handle;
    private final String kind;
    private final boolean local;
    private boolean enabled = true;
    private boolean released;

    MediaTrack(int handle, String kind, boolean local) {
        this.handle = handle;
        this.kind = kind;
        this.local = local;
    }

    int handle() {
        return handle;
    }

    public String getKind() {
        return kind;
    }

    public boolean isVideo() {
        return VIDEO.equals(kind);
    }

    /// Whether this is this device's own camera or microphone.
    public boolean isLocal() {
        return local;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /// Mutes (false) or unmutes (true) the track.
    public void setEnabled(boolean enabled) {
        if (released || enabled == this.enabled) {
            return;
        }
        this.enabled = enabled;
        WebRtc.bridge().setTrackEnabled(handle, enabled);
    }

    /// Switches a local video track between the front and back cameras. Does
    /// nothing for any other track.
    public void switchCamera() {
        if (!released && local && isVideo()) {
            WebRtc.bridge().switchCamera(handle);
        }
    }

    /// Stops the camera or microphone behind a local track. A track cannot be
    /// restarted; open new media instead. Remote tracks end with their
    /// connection, so this does nothing for them.
    public void release() {
        if (released || !local) {
            return;
        }
        released = true;
        WebRtc.bridge().releaseTrack(handle);
    }
}
