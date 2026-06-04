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
package com.codename1.gaming;

import com.codename1.ui.Image;

/// A `Sprite` that cycles through a sequence of frames over time.
///
/// The animation advances in `#onUpdate(double)`, so adding it to a `Scene` whose
/// `Scene#update(double)` is called every frame (or calling `#onUpdate(double)`
/// directly) drives playback. By default it loops; set `#setLooping(boolean)` to
/// false to stop on the last frame.
public class AnimatedSprite extends Sprite {
    private final Image[] frames;
    private double frameDuration;
    private double accumulator;
    private int current;
    private boolean looping = true;
    private boolean playing = true;

    /// Creates an animated sprite from an explicit array of frames.
    ///
    /// #### Parameters
    ///
    /// - `frames`: the frame images, played in order
    ///
    /// - `secondsPerFrame`: how long each frame is shown, in seconds
    public AnimatedSprite(Image[] frames, double secondsPerFrame) {
        if (frames == null || frames.length == 0) {
            throw new IllegalArgumentException("frames is empty");
        }
        this.frames = frames;
        this.frameDuration = secondsPerFrame;
        setImage(frames[0]);
    }

    /// Creates an animated sprite from frames pulled out of a `SpriteSheet`.
    ///
    /// #### Parameters
    ///
    /// - `sheet`: the source sprite sheet
    ///
    /// - `frameIndices`: the linear frame indices to play, in order
    ///
    /// - `secondsPerFrame`: how long each frame is shown, in seconds
    public AnimatedSprite(SpriteSheet sheet, int[] frameIndices, double secondsPerFrame) {
        if (frameIndices == null || frameIndices.length == 0) {
            throw new IllegalArgumentException("frameIndices is empty");
        }
        Image[] f = new Image[frameIndices.length];
        for (int i = 0; i < f.length; i++) {
            f[i] = sheet.getFrame(frameIndices[i]);
        }
        this.frames = f;
        this.frameDuration = secondsPerFrame;
        setImage(f[0]);
    }

    protected void onUpdate(double deltaSeconds) {
        if (!playing || frameDuration <= 0) {
            return;
        }
        accumulator += deltaSeconds;
        while (accumulator >= frameDuration) {
            accumulator -= frameDuration;
            current++;
            if (current >= frames.length) {
                if (looping) {
                    current = 0;
                } else {
                    current = frames.length - 1;
                    playing = false;
                    break;
                }
            }
        }
        setImage(frames[current]);
    }

    /// Starts (or resumes) playback.
    public void play() {
        playing = true;
    }

    /// Pauses playback, keeping the current frame.
    public void pause() {
        playing = false;
    }

    /// Stops playback and rewinds to the first frame.
    public void stop() {
        playing = false;
        current = 0;
        accumulator = 0;
        setImage(frames[0]);
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public double getFrameDuration() {
        return frameDuration;
    }

    public void setFrameDuration(double secondsPerFrame) {
        this.frameDuration = secondsPerFrame;
    }

    /// The index of the frame currently being shown.
    public int getCurrentFrame() {
        return current;
    }

    /// Jumps to the given frame index.
    public void setCurrentFrame(int index) {
        if (index < 0 || index >= frames.length) {
            throw new IndexOutOfBoundsException("frame index " + index);
        }
        current = index;
        accumulator = 0;
        setImage(frames[index]);
    }

    public int getFrameCount() {
        return frames.length;
    }
}
