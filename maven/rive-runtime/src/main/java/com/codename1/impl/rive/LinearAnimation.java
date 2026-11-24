/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import java.util.ArrayList;

/// A timeline animation: keyed objects, frame rate, duration and loop mode.
public class LinearAnimation extends LinearAnimationBase {
    /// Loop value: play once and stop.
    public static final int LOOP_ONE_SHOT = 0;
    /// Loop value: wrap to the start.
    public static final int LOOP_LOOP = 1;
    /// Loop value: reverse at each end.
    public static final int LOOP_PING_PONG = 2;

    private final ArrayList<KeyedObject> keyedObjects = new ArrayList<KeyedObject>();

    /// Appends a keyed object in file order.
    public void addKeyedObject(KeyedObject object) {
        keyedObjects.add(object);
    }

    public int getKeyedObjectCount() {
        return keyedObjects.size();
    }

    public KeyedObject getKeyedObject(int index) {
        return keyedObjects.get(index);
    }

    /// Resolves targets and interpolators, dropping keyed objects that do not resolve.
    public void initialize(Artboard artboard) {
        for (int i = keyedObjects.size() - 1; i >= 0; i--) {
            if (!keyedObjects.get(i).initialize(artboard)) {
                keyedObjects.remove(i);
            }
        }
    }

    private float fpsF() {
        int f = getFps();
        return f <= 0 ? 60.0f : (float) f;
    }

    /// Start of the playable range in seconds.
    public float startSeconds() {
        return (isEnableWorkArea() ? (float) getWorkStart() : 0.0f) / fpsF();
    }

    /// End of the playable range in seconds.
    public float endSeconds() {
        return (float) (isEnableWorkArea() ? getWorkEnd() : getDuration()) / fpsF();
    }

    /// Where playback starts, given the speed's sign.
    public float startTime() {
        return getSpeed() >= 0 ? startSeconds() : endSeconds();
    }

    /// Where playback ends, given the speed's sign.
    public float endTime() {
        return getSpeed() >= 0 ? endSeconds() : startSeconds();
    }

    public float durationSeconds() {
        return Math.abs(endSeconds() - startSeconds());
    }

    /// Applies every keyed property at `seconds`; mix 1 sets, below 1 blends.
    public void apply(Artboard artboard, float seconds, float mix) {
        if (isQuantize()) {
            float f = fpsF();
            seconds = (float) Math.floor(seconds * f) / f;
        }
        int n = keyedObjects.size();
        for (int i = 0; i < n; i++) {
            keyedObjects.get(i).apply(artboard, seconds, mix);
        }
    }
}
