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

/// Any value to a string; numbers are rounded, stripped of trailing zeros
/// and grouped by thousands as the flags say.
public class DataConverterToString extends DataConverterToStringBase {
    private static final int ROUND = 1;
    private static final int TRAILING_ZEROS = 2;
    private static final int COMMAS = 4;

    @Override
    public int outputType() {
        return DataValue.STRING;
    }

    @Override
    public void convert(DataValue v, DataBind bind) {
        if (v.type == DataValue.NUMBER) {
            int flags = getFlags();
            String s = (flags & ROUND) != 0 ? DataValue.fixed(v.number, getDecimals()) : DataValue.formatNumber(v.number);
            if ((flags & TRAILING_ZEROS) != 0) {
                s = DataConverterStringRemoveZeros.removeZeros(s);
            }
            if ((flags & COMMAS) != 0) {
                s = commas(s);
            }
            v.setString(s);
        } else if (v.type == DataValue.COLOR) {
            v.setString(color(v.integer));
        } else {
            v.setString(v.asString());
        }
    }

    private static String color(int argb) {
        String hex = "0123456789ABCDEF";
        StringBuilder sb = new StringBuilder("#");
        for (int shift = 20; shift >= 0; shift -= 4) {
            sb.append(hex.charAt((argb >> shift) & 0xf));
        }
        return sb.toString();
    }

    private static String commas(String s) {
        int dot = s.indexOf('.');
        int end = dot < 0 ? s.length() : dot;
        int start = s.startsWith("-") ? 1 : 0;
        StringBuilder sb = new StringBuilder();
        sb.append(s.substring(0, start));
        for (int i = start; i < end; i++) {
            if (i > start && (end - i) % 3 == 0) {
                sb.append(',');
            }
            sb.append(s.charAt(i));
        }
        sb.append(s.substring(end));
        return sb.toString();
    }
}
