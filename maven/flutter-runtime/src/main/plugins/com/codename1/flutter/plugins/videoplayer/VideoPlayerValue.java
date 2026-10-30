/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.videoplayer;

import com.codename1.flutter.rendering.Size;

import dart.core.Duration;

/** video_player's VideoPlayerValue: an immutable snapshot, copied on every change. */
public class VideoPlayerValue {
    long durationMs;
    long positionMs;
    double width;
    double height;
    boolean initialized;
    boolean playing;
    boolean looping;
    boolean buffering;
    boolean completed;
    double volume = 1.0;
    double playbackSpeed = 1.0;
    String errorDescription;

    VideoPlayerValue() {
    }

    VideoPlayerValue copy() {
        VideoPlayerValue v = new VideoPlayerValue();
        v.durationMs = durationMs;
        v.positionMs = positionMs;
        v.width = width;
        v.height = height;
        v.initialized = initialized;
        v.playing = playing;
        v.looping = looping;
        v.buffering = buffering;
        v.completed = completed;
        v.volume = volume;
        v.playbackSpeed = playbackSpeed;
        v.errorDescription = errorDescription;
        return v;
    }

    public Duration duration() {
        return Duration.ofMicroseconds(durationMs * 1000);
    }

    public Duration position() {
        return Duration.ofMicroseconds(positionMs * 1000);
    }

    public Size size() {
        return new Size(width, height);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isLooping() {
        return looping;
    }

    public boolean isBuffering() {
        return buffering;
    }

    public boolean isCompleted() {
        return completed;
    }

    public double volume() {
        return volume;
    }

    public double playbackSpeed() {
        return playbackSpeed;
    }

    public boolean hasError() {
        return errorDescription != null;
    }

    public String errorDescription() {
        return errorDescription;
    }

    /** Width over height, 1.0 until the size is known, as the plugin answers. */
    public double aspectRatio() {
        if (!initialized || width <= 0 || height <= 0) {
            return 1.0;
        }
        return width / height;
    }

    @Override
    public String toString() {
        return "VideoPlayerValue(duration: " + durationMs + "ms, position: " + positionMs + "ms, isInitialized: "
                + initialized + ", isPlaying: " + playing + ", isLooping: " + looping + ")";
    }
}
