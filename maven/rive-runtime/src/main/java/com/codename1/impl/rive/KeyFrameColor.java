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

/// A keyframe holding an ARGB color.
public class KeyFrameColor extends KeyFrameColorBase {
    private static int lerp8(int a, int b, float mix) {
        float v = a * (1.0f - mix) + b * mix;
        if (v < 0) {
            v = 0;
        } else if (v > 255) {
            v = 255;
        }
        return (int) Math.floor(v + 0.5f);
    }

    /// Interpolates two ARGB colors channel by channel.
    public static int colorLerp(int from, int to, float mix) {
        int a = lerp8((from >>> 24) & 0xff, (to >>> 24) & 0xff, mix);
        int r = lerp8((from >> 16) & 0xff, (to >> 16) & 0xff, mix);
        int g = lerp8((from >> 8) & 0xff, (to >> 8) & 0xff, mix);
        int b = lerp8(from & 0xff, to & 0xff, mix);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void applyColor(Core object, int propertyKey, float mix, int value) {
        if (mix == 1.0f) {
            object.setColorProperty(propertyKey, value);
        } else {
            object.setColorProperty(propertyKey, colorLerp(object.getColorProperty(propertyKey), value, mix));
        }
    }

    @Override
    public void apply(Core object, int propertyKey, float mix) {
        applyColor(object, propertyKey, mix, getValue());
    }

    @Override
    public void applyInterpolation(Core object, int propertyKey, float currentTime, KeyFrame nextFrame, float mix) {
        if (!(nextFrame instanceof KeyFrameColor)) {
            apply(object, propertyKey, mix);
            return;
        }
        float f = ease(factor(currentTime, nextFrame));
        int v = colorLerp(getValue(), ((KeyFrameColor) nextFrame).getValue(), f);
        applyColor(object, propertyKey, mix, v);
    }
}
