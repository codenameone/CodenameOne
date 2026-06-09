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
package com.codename1.impl.windows;

import com.codename1.media.AbstractMedia;
import com.codename1.ui.Component;

/**
 * A {@link com.codename1.media.Media} backed by the Windows Media Foundation
 * Media Engine. The native peer (cn1_windows_media.cpp) owns the
 * {@code IMFMediaEngine}; this class is the thin Java surface that
 * {@code MediaManager} drives.
 */
class WindowsMedia extends AbstractMedia {
    private long peer;
    private final Runnable onCompletion;
    private int volume = 100;

    WindowsMedia(long peer, Runnable onCompletion) {
        this.peer = peer;
        this.onCompletion = onCompletion;
        if (onCompletion != null) {
            addMediaCompletionHandler(onCompletion);
        }
    }

    @Override
    protected void playImpl() {
        if (peer != 0) {
            WindowsNative.mediaPlay(peer);
            fireMediaStateChange(State.Playing);
        }
    }

    @Override
    protected void pauseImpl() {
        if (peer != 0) {
            WindowsNative.mediaPause(peer);
            fireMediaStateChange(State.Paused);
        }
    }

    @Override
    public void prepare() {
        // The engine loads asynchronously as soon as the source is set in
        // createMedia(); there is nothing extra to prepare.
    }

    @Override
    public void cleanup() {
        if (peer != 0) {
            WindowsNative.mediaDestroy(peer);
            peer = 0;
        }
    }

    @Override
    public int getTime() {
        return peer != 0 ? WindowsNative.mediaGetTime(peer) : 0;
    }

    @Override
    public void setTime(int time) {
        if (peer != 0) {
            WindowsNative.mediaSetTime(peer, time);
        }
    }

    @Override
    public int getDuration() {
        return peer != 0 ? WindowsNative.mediaGetDuration(peer) : 0;
    }

    @Override
    public int getVolume() {
        return volume;
    }

    @Override
    public void setVolume(int vol) {
        volume = vol;
        if (peer != 0) {
            WindowsNative.mediaSetVolume(peer, vol);
        }
    }

    @Override
    public boolean isPlaying() {
        return peer != 0 && WindowsNative.mediaIsPlaying(peer);
    }

    @Override
    public Component getVideoComponent() {
        // Audio playback only for now; a video peer (DXGI swap chain +
        // TransferVideoFrame) is future work.
        return null;
    }

    @Override
    public boolean isVideo() {
        return peer != 0 && WindowsNative.mediaIsVideo(peer);
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
    }

    @Override
    public boolean isNativePlayerMode() {
        return false;
    }

    @Override
    public void setNativePlayerMode(boolean nativePlayer) {
    }

    @Override
    public void setVariable(String key, Object value) {
    }

    @Override
    public Object getVariable(String key) {
        return null;
    }
}
