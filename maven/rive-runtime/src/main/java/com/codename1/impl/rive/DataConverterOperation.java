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

/// Applies an arithmetic operation with an operand to a number.
public class DataConverterOperation extends DataConverterOperationBase {
    @Override
    public int outputType() {
        return DataValue.NUMBER;
    }

    /// The operand; subclasses supply it.
    protected float operand() {
        return 0;
    }

    @Override
    public void convert(DataValue v, DataBind bind) {
        float in = v.type == DataValue.NUMBER ? v.number : 0;
        v.setNumber(operate(getOperationType(), in, operand()));
    }

    @Override
    public void reverseConvert(DataValue v, DataBind bind) {
        float in = v.type == DataValue.NUMBER ? v.number : 0;
        float o = operand();
        switch (getOperationType()) {
            case 0:
                v.setNumber(in - o);
                break;
            case 1:
                v.setNumber(in + o);
                break;
            case 2:
                v.setNumber(in / o);
                break;
            case 3:
                v.setNumber(in * o);
                break;
            default:
                v.setNumber(in);
                break;
        }
    }

    /// Upstream `ArithmeticOperation` applied to `in` and `value`.
    public static float operate(int op, float in, float value) {
        switch (op) {
            case 0:
                return in + value;
            case 1:
                return in - value;
            case 2:
                return in * value;
            case 3:
                return in / value;
            case 4:
                return positiveMod(in, value);
            case 5:
                return (float) Math.sqrt(in);
            case 6:
                return (float) com.codename1.util.MathUtil.pow(in, value);
            case 7:
                return (float) com.codename1.util.MathUtil.exp(in);
            case 8:
                return (float) com.codename1.util.MathUtil.log(in);
            case 9:
                return (float) Math.cos(in);
            case 10:
                return (float) Math.sin(in);
            case 11:
                return (float) Math.tan(in);
            case 12:
                return (float) com.codename1.util.MathUtil.acos(in);
            case 13:
                return (float) com.codename1.util.MathUtil.asin(in);
            case 14:
                return (float) com.codename1.util.MathUtil.atan(in);
            case 15:
                return (float) com.codename1.util.MathUtil.atan2(in, value);
            case 16:
                return (float) roundHalfAway(in);
            case 17:
                return (float) Math.floor(in);
            case 18:
                return (float) Math.ceil(in);
            default:
                return value;
        }
    }

    /// `math::positive_mod`.
    public static float positiveMod(float a, float b) {
        if (b == 0) {
            return 0;
        }
        float r = a % b;
        return r < 0 ? r + b : r;
    }

    /// C++ `std::round`: halves round away from zero, so -2.5 is -3 (a plain
    /// `floor(x + 0.5)` gives -2).
    public static double roundHalfAway(double x) {
        double r = Math.floor(Math.abs(x) + 0.5);
        return x < 0 ? -r : r;
    }
}
