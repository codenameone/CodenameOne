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

/// Port of `DistanceConstraint`: keeps the parent closer than, further than
/// or exactly at a distance from the target.
public class DistanceConstraint extends DistanceConstraintBase {
    private static final int MODE_CLOSER = 0;
    private static final int MODE_FURTHER = 1;

    @Override
    public void constrain(TransformComponent component) {
        TransformComponent tgt = target;
        if (tgt == null || tgt.isCollapsed()) {
            return;
        }
        float[] world = component.getWorldTransform().v;
        float[] tw = tgt.getWorldTransform().v;
        float tx = tw[4];
        float ty = tw[5];
        float ox = world[4];
        float oy = world[5];
        float dx = ox - tx;
        float dy = oy - ty;
        float current = (float) Math.sqrt(dx * dx + dy * dy);
        if (mModeValue == MODE_CLOSER) {
            if (current < mDistance) {
                return;
            }
        } else if (mModeValue == MODE_FURTHER && current > mDistance) {
            return;
        }
        if (current < 0.001f) {
            return;
        }
        float k = mDistance / current;
        float px = tx + dx * k;
        float py = ty + dy * k;
        float s = mStrength;
        world[4] = ox + (px - ox) * s;
        world[5] = oy + (py - oy) * s;
    }

    @Override
    protected void distanceChanged() {
        markConstraintDirty();
    }

    @Override
    protected void modeValueChanged() {
        markConstraintDirty();
    }
}
