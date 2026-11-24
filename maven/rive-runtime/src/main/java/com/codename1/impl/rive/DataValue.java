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

/// A value flowing through a data bind and its converters: a port of
/// rive-runtime's `DataValue` family folded into one mutable holder, so an
/// update allocates nothing.
public final class DataValue {
    public static final int NONE = 0;
    public static final int NUMBER = 1;
    public static final int STRING = 2;
    public static final int BOOLEAN = 3;
    public static final int COLOR = 4;
    public static final int ENUM = 5;
    public static final int TRIGGER = 6;
    public static final int LIST = 7;
    /// A reference to a view model instance (upstream `DataValueViewModel`).
    public static final int VIEW_MODEL = 8;

    public int type;
    public float number;
    public String string = "";
    public boolean bool;
    /// Color, enum index, trigger count or list length.
    public int integer;
    public DataEnum dataEnum;
    /// The referenced instance when `type` is `VIEW_MODEL`.
    public ViewModelInstance viewModel;
    /// The list a converter produced when `type` is `LIST` (upstream
    /// `DataValueList`); null when the list is the bind's own source.
    public ViewModelInstanceList list;

    public void setNumber(float v) {
        type = NUMBER;
        number = v;
    }

    public void setString(String v) {
        type = STRING;
        string = v == null ? "" : v;
    }

    public void setBoolean(boolean v) {
        type = BOOLEAN;
        bool = v;
    }

    public void setColor(int v) {
        type = COLOR;
        integer = v;
    }

    public void setEnum(DataEnum e, int index) {
        type = ENUM;
        dataEnum = e;
        integer = index;
    }

    public void setTrigger(int v) {
        type = TRIGGER;
        integer = v;
    }

    public void setList(int length) {
        type = LIST;
        integer = length;
        list = null;
    }

    /// A list a converter made: `l`'s instances are the items.
    public void setList(ViewModelInstanceList l) {
        type = LIST;
        integer = l.getInstances().size();
        list = l;
    }

    public void setViewModel(ViewModelInstance v) {
        type = VIEW_MODEL;
        viewModel = v;
    }

    public void copyFrom(DataValue o) {
        type = o.type;
        number = o.number;
        string = o.string;
        bool = o.bool;
        integer = o.integer;
        dataEnum = o.dataEnum;
        viewModel = o.viewModel;
        list = o.list;
    }

    /// Whether this holds the same value as `o`.
    public boolean sameAs(DataValue o) {
        if (type != o.type) {
            return false;
        }
        switch (type) {
            case NUMBER:
                return Float.floatToIntBits(number) == Float.floatToIntBits(o.number);
            case STRING:
                return string.equals(o.string);
            case BOOLEAN:
                return bool == o.bool;
            case VIEW_MODEL:
                return viewModel == o.viewModel; // NOPMD CompareObjectsWithEquals - identity is the intent
            default:
                return integer == o.integer;
        }
    }

    /// The value as a number, the way the runtime coerces it.
    public float asNumber() {
        switch (type) {
            case NUMBER:
                return number;
            case BOOLEAN:
                return bool ? 1 : 0;
            case STRING:
                return parse(string);
            default:
                return integer;
        }
    }

    /// The value as a string.
    public String asString() {
        switch (type) {
            case STRING:
                return string;
            case NUMBER:
                return formatNumber(number);
            case BOOLEAN:
                return bool ? "true" : "false";
            case ENUM:
                if (dataEnum != null) {
                    DataEnumValue v = dataEnum.getEnumValue(integer);
                    if (v != null) {
                        return v.getValue() != null && v.getValue().length() > 0 ? v.getValue() : v.getKey();
                    }
                }
                return "";
            default:
                return String.valueOf(integer);
        }
    }

    /// `std::to_string` of a float: six decimals.
    public static String formatNumber(float v) {
        return fixed(v, 6);
    }

    /// `v` with exactly `decimals` digits after the point.
    public static String fixed(float v, int decimals) {
        if (Float.isNaN(v) || Float.isInfinite(v)) {
            return String.valueOf(v);
        }
        int d = decimals < 0 ? 0 : decimals > 9 ? 9 : decimals;
        long scale = 1;
        for (int i = 0; i < d; i++) {
            scale *= 10;
        }
        double scaled = Math.abs((double) v) * scale;
        long n = (long) Math.floor(scaled + 0.5);
        StringBuilder sb = new StringBuilder();
        if (v < 0 && n != 0) {
            sb.append('-');
        }
        sb.append(n / scale);
        if (d > 0) {
            sb.append('.');
            String frac = String.valueOf(n % scale);
            for (int i = frac.length(); i < d; i++) {
                sb.append('0');
            }
            sb.append(frac);
        }
        return sb.toString();
    }

    /// A float parsed leniently; 0 when `s` is not a number.
    public static float parse(String s) {
        if (s == null) {
            return 0;
        }
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
