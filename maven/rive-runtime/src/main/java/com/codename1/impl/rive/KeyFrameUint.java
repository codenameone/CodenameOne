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

/// A keyframe holding an unsigned integer.
public class KeyFrameUint extends KeyFrameUintBase {
    private static void applyUint(Core object, int propertyKey, float mix, float value) {
        if (mix != 1.0f) {
            value = mixDouble(object.getUintProperty(propertyKey), value, mix);
        }
        object.setUintProperty(propertyKey, (int) Math.floor(value + 0.5f));
    }

    @Override
    public void apply(Core object, int propertyKey, float mix) {
        applyUint(object, propertyKey, mix, getValue());
    }

    @Override
    public void applyInterpolation(Core object, int propertyKey, float currentTime, KeyFrame nextFrame, float mix) {
        if (!(nextFrame instanceof KeyFrameUint)) {
            apply(object, propertyKey, mix);
            return;
        }
        float from = getValue();
        float to = ((KeyFrameUint) nextFrame).getValue();
        float f = factor(currentTime, nextFrame);
        KeyFrameInterpolator interp = getInterpolator();
        applyUint(object, propertyKey, mix, interp != null ? interp.transformValue(from, to, f) : from + (to - from) * f);
    }
}
