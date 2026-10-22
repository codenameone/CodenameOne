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

/// The camera and microphone this device opened with
/// [WebRtc#openUserMedia(boolean, boolean, boolean)].
public final class LocalMedia {
    private final MediaTrack audio;
    private final MediaTrack video;

    LocalMedia(MediaTrack audio, MediaTrack video) {
        this.audio = audio;
        this.video = video;
    }

    /// The microphone, or null when none was asked for.
    public MediaTrack getAudio() {
        return audio;
    }

    /// The camera, or null when none was asked for.
    public MediaTrack getVideo() {
        return video;
    }

    /// Stops both.
    public void release() {
        if (audio != null) {
            audio.release();
        }
        if (video != null) {
            video.release();
        }
    }
}
