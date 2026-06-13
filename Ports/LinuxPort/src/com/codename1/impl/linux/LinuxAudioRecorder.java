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
package com.codename1.impl.linux;

import com.codename1.media.AbstractMedia;
import com.codename1.ui.Component;

/**
 * A recording {@link com.codename1.media.Media} backed by the Linux waveIn
 * capture API (writes a PCM WAV file). {@code play()} starts recording from the
 * default microphone, {@code pause()} / {@code cleanup()} stops and finalizes
 * the file. Native peer: {@code cn1_linux_audiorec.c}.
 */
class LinuxAudioRecorder extends AbstractMedia {
    private final String path;
    private final int sampleRate;
    private final int channels;
    private long peer;
    private long startTime;

    LinuxAudioRecorder(String path, int sampleRate, int channels) {
        this.path = path;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    @Override
    protected void playImpl() {
        if (peer == 0 && path != null) {
            peer = LinuxNative.audioRecStart(path, sampleRate, channels);
            if (peer != 0) {
                startTime = System.currentTimeMillis();
                fireMediaStateChange(State.Playing);
            }
        }
    }

    @Override
    protected void pauseImpl() {
        stop();
    }

    @Override
    public void cleanup() {
        stop();
    }

    private void stop() {
        if (peer != 0) {
            LinuxNative.audioRecStop(peer);
            peer = 0;
            fireMediaStateChange(State.Paused);
        }
    }

    @Override
    public void prepare() {
    }

    @Override
    public int getTime() {
        return peer != 0 ? (int) (System.currentTimeMillis() - startTime) : 0;
    }

    @Override
    public void setTime(int time) {
    }

    @Override
    public int getDuration() {
        return getTime();
    }

    @Override
    public int getVolume() {
        return 100;
    }

    @Override
    public void setVolume(int vol) {
    }

    @Override
    public boolean isPlaying() {
        return peer != 0;
    }

    @Override
    public Component getVideoComponent() {
        return null;
    }

    @Override
    public boolean isVideo() {
        return false;
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
