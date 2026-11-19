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

/// Eases the bound value towards each new one over `duration`, advanced
/// with the artboard (upstream `DataConverterInterpolator` and its
/// `InterpolatorAdvancer`). A new value arriving mid tween starts a
/// smoothing animation from the running one, as upstream does.
public class DataConverterInterpolator extends DataConverterInterpolatorBase {
    private KeyFrameInterpolator interpolator;
    private boolean resolved;
    private int advanceCount;
    private boolean initialized;
    private boolean cannotEase;
    private boolean smoothing;
    private int valueType;
    // Animation A and B: from, to and elapsed seconds; a number or a color.
    private final DataValue fromA = new DataValue();
    private final DataValue toA = new DataValue();
    private float elapsedA;
    private final DataValue fromB = new DataValue();
    private final DataValue toB = new DataValue();
    private float elapsedB;
    private final DataValue current = new DataValue();

    private KeyFrameInterpolator activeInterpolator() {
        if (!resolved) {
            resolved = true;
            RiveFileContext f = getFile();
            Core c = f == null ? null : f.getInterpolator(getInterpolatorId());
            if (c instanceof KeyFrameInterpolator) {
                interpolator = (KeyFrameInterpolator) c;
            }
        }
        return interpolator;
    }

    private boolean isFirstRun() {
        return advanceCount < 2;
    }

    private void interpolate(DataValue from, DataValue to, float f, DataValue store) {
        store.type = valueType;
        if (valueType == DataValue.COLOR) {
            store.integer = KeyFrameColor.colorLerp(from.integer, to.integer, f);
        } else {
            store.number = from.number + (to.number - from.number) * f;
        }
    }

    private boolean same(DataValue a, DataValue b) {
        return valueType == DataValue.COLOR ? a.integer == b.integer
                : Float.floatToIntBits(a.number) == Float.floatToIntBits(b.number);
    }

    private void copyValue(DataValue from, DataValue to) {
        to.type = valueType;
        to.number = from.number;
        to.integer = from.integer;
    }

    private float factor(float elapsed) {
        float d = getDuration();
        float f = Math.min(1f, d > 0 ? elapsed / d : 1f);
        KeyFrameInterpolator k = activeInterpolator();
        return k == null ? f : k.transform(f);
    }

    private void advanceAnimationData(float elapsedTime) {
        float d = getDuration();
        if (smoothing) {
            float f = factor(elapsedA);
            interpolate(fromA, toA, f, fromB);
            if (f == 1) {
                copyValue(fromB, fromA);
                copyValue(toB, toA);
                elapsedA = elapsedB;
                smoothing = false;
            } else {
                elapsedA += elapsedTime;
            }
        }
        DataValue to = smoothing ? toB : toA;
        float elapsed = smoothing ? elapsedB : elapsedA;
        if (elapsed >= d) {
            copyValue(to, current);
            if (smoothing) {
                smoothing = false;
                copyValue(fromB, fromA);
                copyValue(toB, toA);
                elapsedA = 0;
                elapsedB = 0;
            } else {
                elapsedA = 0;
            }
            return;
        }
        elapsed += elapsedTime;
        if (smoothing) {
            elapsedB = elapsed;
            interpolate(fromB, toB, factor(elapsed), current);
        } else {
            elapsedA = elapsed;
            interpolate(fromA, toA, factor(elapsed), current);
        }
    }

    /// Upstream `DataConverterInterpolator::advance`; answers whether the
    /// bind must convert again.
    @Override
    public boolean advance(float elapsedTime) {
        boolean dirty = super.advance(elapsedTime);
        if (advanceCount < 2 && elapsedTime > 0) {
            advanceCount++;
        }
        if (!initialized) {
            return true;
        }
        if (same(smoothing ? toB : toA, current) || elapsedTime == 0) {
            return dirty;
        }
        float prev = smoothing ? elapsedB : elapsedA;
        advanceAnimationData(elapsedTime);
        return prev < getDuration() || dirty;
    }

    private void resetValues(DataValue input) {
        if (smoothing) {
            copyValue(input, fromB);
            copyValue(input, toB);
        } else {
            copyValue(input, fromA);
            copyValue(input, toA);
        }
        copyValue(input, current);
    }

    private void updateValues(DataValue input) {
        DataValue to = smoothing ? toB : toA;
        if (same(input, to)) {
            return;
        }
        float elapsed = smoothing ? elapsedB : elapsedA;
        if (elapsed != 0 && !same(current, to)) {
            if (smoothing) {
                copyValue(fromB, fromA);
                copyValue(toB, toA);
                elapsedA = elapsedB;
            }
            smoothing = true;
        } else {
            smoothing = false;
        }
        if (smoothing) {
            copyValue(current, fromB);
            copyValue(input, toB);
            elapsedB = 0;
        } else {
            copyValue(current, fromA);
            copyValue(input, toA);
            elapsedA = 0;
        }
    }

    @Override
    public void convert(DataValue v, DataBind bind) {
        if (getDuration() == 0 && initialized) {
            resetValues(v);
            smoothing = false;
            elapsedA = 0;
            elapsedB = 0;
            return;
        }
        if (!initialized) {
            if (v.type != DataValue.NUMBER && v.type != DataValue.COLOR) {
                cannotEase = true;
                return;
            }
            valueType = v.type;
            initialized = true;
            cannotEase = false;
        }
        if (v.type != valueType) {
            return;
        }
        if (isFirstRun()) {
            resetValues(v);
        } else {
            updateValues(v);
        }
        copyValue(current, v);
    }

    @Override
    public void reverseConvert(DataValue v, DataBind bind) {
        convert(v, bind);
    }

    /// Whether the last input could not be eased (not a number or a color).
    public boolean cannotEase() {
        return cannotEase;
    }
}
