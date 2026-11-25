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

/// Maps a number from an input range to an output range, with optional
/// clamping, wrapping, reversal and an interpolator.
public class DataConverterRangeMapper extends DataConverterRangeMapperBase {
    private static final int CLAMP_LOWER = 1;
    private static final int CLAMP_UPPER = 2;
    private static final int MODULO = 4;
    private static final int REVERSE = 8;
    private KeyFrameInterpolator interpolator;
    private boolean resolved;

    @Override
    public int outputType() {
        return DataValue.NUMBER;
    }

    private void resolveInterpolator() {
        if (resolved) {
            return;
        }
        resolved = true;
        RiveFileContext f = getFile();
        if (f != null) {
            Core c = f.getInterpolator(getInterpolatorId());
            if (c instanceof KeyFrameInterpolator) {
                interpolator = (KeyFrameInterpolator) c;
            }
        }
    }

    @Override
    public void convert(DataValue v, DataBind bind) {
        range(v, getMinInput(), getMaxInput(), getMinOutput(), getMaxOutput());
    }

    @Override
    public void reverseConvert(DataValue v, DataBind bind) {
        range(v, getMinOutput(), getMaxOutput(), getMinInput(), getMaxInput());
    }

    private void range(DataValue v, float minInput, float maxInput, float minOutput, float maxOutput) {
        if (v.type != DataValue.NUMBER) {
            v.setNumber(0);
            return;
        }
        if (minOutput == maxOutput) {
            v.setNumber(minOutput);
            return;
        }
        resolveInterpolator();
        int flags = getFlags();
        float value = v.number;
        if (value < minInput && (flags & CLAMP_LOWER) != 0) {
            value = minInput;
        } else if (value > maxInput && (flags & CLAMP_UPPER) != 0) {
            value = maxInput;
        }
        if ((value < minInput || value > maxInput) && (flags & MODULO) != 0) {
            value = Math.abs(DataConverterOperation.positiveMod(value, maxInput - minInput) + minInput);
        }
        float perc = (value - minInput) / (maxInput - minInput);
        if ((flags & REVERSE) != 0) {
            perc = 1 - perc;
        }
        if (interpolator != null && perc > 0 && perc < 1) {
            perc = interpolator.transform(perc);
        } else if (getInterpolationType() == 0) {
            perc = perc <= 0 ? 0 : 1;
        }
        v.setNumber(perc * maxOutput + (1 - perc) * minOutput);
    }
}
