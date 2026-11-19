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

/// Removes the trailing zeros of a decimal string.
public class DataConverterStringRemoveZeros extends DataConverterStringRemoveZerosBase {
    @Override
    public int outputType() {
        return DataValue.STRING;
    }

    /// Upstream `removeZeros`.
    public static String removeZeros(String value) {
        if (value.indexOf('.') < 0) {
            return value;
        }
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '0') {
            end--;
        }
        String out = value.substring(0, end);
        if (out.endsWith(".")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    @Override
    public void convert(DataValue v, DataBind bind) {
        v.setString(v.type == DataValue.STRING ? removeZeros(v.string) : "");
    }
}
