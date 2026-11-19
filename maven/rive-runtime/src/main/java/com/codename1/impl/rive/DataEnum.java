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

/// An enum a view model property can take values of.
public class DataEnum extends DataEnumBase {
    private final ArrayList<DataEnumValue> values = new ArrayList<DataEnumValue>();

    public void addValue(DataEnumValue v) {
        values.add(v);
    }

    public ArrayList<DataEnumValue> getValues() {
        return values;
    }

    /// The value at `index`, or null.
    public DataEnumValue getEnumValue(int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    /// The index of the value whose key or value is `name`, or -1.
    public int indexOf(String name) {
        if (name == null) {
            return -1;
        }
        for (int i = 0; i < values.size(); i++) {
            DataEnumValue v = values.get(i);
            if (name.equals(v.getKey()) || name.equals(v.getValue())) {
                return i;
            }
        }
        return -1;
    }
}
