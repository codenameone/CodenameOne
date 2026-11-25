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

/// The keyframes of one property of one object in a linear animation.
public class KeyedProperty extends KeyedPropertyBase {
    private final ArrayList<KeyFrame> keyFrames = new ArrayList<KeyFrame>();

    /// Appends a keyframe in file order.
    public void addKeyFrame(KeyFrame keyFrame) {
        keyFrames.add(keyFrame);
    }

    public int getKeyFrameCount() {
        return keyFrames.size();
    }

    public KeyFrame getKeyFrame(int index) {
        return keyFrames.get(index);
    }

    /// Resolves keyframe references, dropping keyframes that fail; false when none remain.
    public boolean initialize(Artboard artboard) {
        for (int i = keyFrames.size() - 1; i >= 0; i--) {
            if (!keyFrames.get(i).initialize(artboard)) {
                keyFrames.remove(i);
            }
        }
        return !keyFrames.isEmpty();
    }

    /// The index of the first keyframe at or after `seconds`.
    public int closestFrameIndex(float seconds, int exactOffset) {
        int start = 0;
        int end = keyFrames.size() - 1;
        if (end < 0 || seconds > keyFrames.get(end).getSeconds()) {
            return end + 1;
        }
        while (start <= end) {
            int mid = (start + end) >> 1;
            float closest = keyFrames.get(mid).getSeconds();
            if (closest < seconds) {
                start = mid + 1;
            } else if (closest > seconds) {
                end = mid - 1;
            } else {
                return mid + exactOffset;
            }
        }
        return start;
    }

    /// Applies this property to `object` at time `seconds`.
    public void apply(Core object, float seconds, float mix) {
        int count = keyFrames.size();
        if (count == 0) {
            return;
        }
        int pk = getPropertyKey();
        int idx = closestFrameIndex(seconds, 0);
        if (idx == 0) {
            keyFrames.get(0).apply(object, pk, mix);
        } else if (idx < count) {
            KeyFrame from = keyFrames.get(idx - 1);
            KeyFrame to = keyFrames.get(idx);
            if (seconds == to.getSeconds()) {
                to.apply(object, pk, mix);
            } else if (!(from instanceof InterpolatingKeyFrame)
                    || ((InterpolatingKeyFrame) from).getInterpolationType() == 0) {
                from.apply(object, pk, mix);
            } else {
                from.applyInterpolation(object, pk, seconds, to, mix);
            }
        } else {
            keyFrames.get(idx - 1).apply(object, pk, mix);
        }
    }
}
