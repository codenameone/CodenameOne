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

/// Rive `TextModifierRange`: which characters, words or lines a modifier
/// group affects and how strongly, with a linear falloff at either end.
///
/// Not ported: a range's interpolator child (falloff is always linear) and
/// `runId` (a range always spans the whole text).
public class TextModifierRange extends TextModifierRangeBase {
    public static final int UNITS_CHARACTERS = 0;
    public static final int UNITS_CHARACTERS_EXCLUDING_SPACES = 1;
    public static final int UNITS_WORDS = 2;
    public static final int UNITS_LINES = 3;

    private float indexFrom;
    private float indexTo;
    private float indexFalloffFrom;
    private float indexFalloffTo;

    private float coverageAt(float t) {
        if (indexTo < indexFrom || t < indexFrom || t > indexTo) {
            return 0f;
        }
        if (t < indexFalloffFrom) {
            float range = Math.max(0f, indexFalloffFrom - indexFrom);
            return range == 0f ? 1f : Math.max(0f, t - indexFrom) / range;
        }
        if (t > indexFalloffTo) {
            float range = Math.max(0f, indexTo - indexFalloffTo);
            return range == 0f ? 1f : 1f - Math.min(1f, (t - indexFalloffTo) / range);
        }
        return 1f;
    }

    /// Applies this range to `coverage`, one entry per code point.
    /// `unitOf[i]` is the unit index of code point `i`, or -1 when the code
    /// point belongs to no unit (a space between words).
    void computeCoverage(float[] coverage, int[] unitOf, int count, int unitCount) {
        float o = mOffset;
        if (mTypeValue == 1) {
            indexFrom = mModifyFrom + o;
            indexTo = mModifyTo + o;
            indexFalloffFrom = mFalloffFrom + o;
            indexFalloffTo = mFalloffTo + o;
        } else {
            indexFrom = unitCount * (mModifyFrom + o);
            indexTo = unitCount * (mModifyTo + o);
            indexFalloffFrom = unitCount * (mFalloffFrom + o);
            indexFalloffTo = unitCount * (mFalloffTo + o);
        }
        for (int i = 0; i < count; i++) {
            int u = unitOf[i];
            if (u < 0) {
                coverage[i] = 0f;
                continue;
            }
            float c = mStrength * coverageAt(u + 0.5f);
            float current = coverage[i];
            switch (mModeValue) {
                case 1:
                    current -= c;
                    break;
                case 2:
                    current *= c;
                    break;
                case 3:
                    current = Math.min(current, c);
                    break;
                case 4:
                    current = Math.max(current, c);
                    break;
                case 5:
                    current = Math.abs(current - c);
                    break;
                default:
                    current += c;
                    break;
            }
            coverage[i] = mClamp ? Math.max(0f, Math.min(1f, current)) : current;
        }
    }

    private void changed() {
        if (parent != null && parent.getParent() instanceof Text) {
            parent.getParent().addDirt(DIRT_TEXT_SHAPE);
        }
    }

    @Override
    protected void modifyFromChanged() {
        changed();
    }

    @Override
    protected void modifyToChanged() {
        changed();
    }

    @Override
    protected void modeValueChanged() {
        changed();
    }

    @Override
    protected void strengthChanged() {
        changed();
    }

    @Override
    protected void unitsValueChanged() {
        changed();
    }

    @Override
    protected void typeValueChanged() {
        changed();
    }

    @Override
    protected void clampChanged() {
        changed();
    }

    @Override
    protected void falloffFromChanged() {
        changed();
    }

    @Override
    protected void falloffToChanged() {
        changed();
    }

    @Override
    protected void offsetChanged() {
        changed();
    }
}
