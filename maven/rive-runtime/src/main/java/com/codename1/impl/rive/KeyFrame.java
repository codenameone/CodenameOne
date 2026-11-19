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

/// A keyframe: a value of one property at one frame of a linear animation.
public class KeyFrame extends KeyFrameBase {
    private float seconds;

    /// Converts the stored frame number to seconds at the animation's frame rate.
    public void computeSeconds(int fps) {
        seconds = fps == 0 ? 0 : getFrame() / (float) fps;
    }

    /// The time of this keyframe in seconds, valid after `computeSeconds`.
    public float getSeconds() {
        return seconds;
    }

    /// Resolves references after the artboard is imported; false drops the frame.
    public boolean initialize(Artboard artboard) {
        return true;
    }

    /// Applies this keyframe's value to `object`, mixing by `mix`.
    public void apply(Core object, int propertyKey, float mix) {
    }

    /// Applies the value interpolated between this frame and `nextFrame` at `currentTime`.
    public void applyInterpolation(Core object, int propertyKey, float currentTime, KeyFrame nextFrame, float mix) {
        apply(object, propertyKey, mix);
    }

    /// Mixes two doubles the way the C++ runtime's mixKeyedDouble does.
    static float mixDouble(float current, float value, float mix) {
        if (mix == 1.0f) {
            return value;
        }
        return current * (1.0f - mix) + value * mix;
    }
}
