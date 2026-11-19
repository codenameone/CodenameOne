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

/// A keyframe that can ease towards the next keyframe.
public class InterpolatingKeyFrame extends InterpolatingKeyFrameBase {
    private KeyFrameInterpolator interpolator;

    /// The resolved interpolator, or null for linear interpolation.
    public KeyFrameInterpolator getInterpolator() {
        return interpolator;
    }

    /// Resolves `interpolatorId` against the artboard; false when it is dangling.
    @Override
    public boolean initialize(Artboard artboard) {
        interpolator = null;
        int id = getInterpolatorId();
        if (id != -1) {
            Core c = artboard.resolve(id);
            if (!(c instanceof KeyFrameInterpolator)) {
                return false;
            }
            interpolator = (KeyFrameInterpolator) c;
            interpolator.initialize();
        }
        return true;
    }

    /// The eased progress factor for `f` in 0..1.
    protected float ease(float f) {
        return interpolator == null ? f : interpolator.transform(f);
    }

    /// The factor between this frame and `next` at `time`.
    protected float factor(float time, KeyFrame next) {
        float span = next.getSeconds() - getSeconds();
        return span == 0 ? 1 : (time - getSeconds()) / span;
    }
}
