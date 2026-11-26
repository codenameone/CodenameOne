/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.functions;

/// The typed record a function reads: the cart's scalars and the target's
/// lists as columns, one array per field. Built once per evaluation; the
/// interpreter only indexes into it. A string never read as null: unset
/// cells are empty.
public final class FunctionInput {
    final Target target;
    final long[] longs = new long[Target.SCALARS.length];
    final String[] strings = new String[Target.SCALARS.length];
    final int[] sizes;
    final long[][][] numbers;
    final String[][][] texts;

    FunctionInput(Target target, int[] sizes) {
        this.target = target;
        int lists = target.listCount();
        this.sizes = new int[lists];
        numbers = new long[lists][][];
        texts = new String[lists][][];
        for (int iter = 0; iter < Target.SCALARS.length; iter++) {
            strings[iter] = "";
        }
        for (int list = 0; list < lists; list++) {
            int rows = list < sizes.length ? Math.max(0, sizes[list]) : 0;
            this.sizes[list] = rows;
            int count = target.fieldCount(list);
            numbers[list] = new long[count][];
            texts[list] = new String[count][];
            for (int f = 0; f < count; f++) {
                if (target.fieldType(list, f) == Target.STRING) {
                    String[] col = new String[rows];
                    for (int r = 0; r < rows; r++) {
                        col[r] = "";
                    }
                    texts[list][f] = col;
                } else {
                    numbers[list][f] = new long[rows];
                }
            }
            if (count > 0 && target.fieldType(list, 0) == Target.LONG) {
                // Field 0 of every list is its row's index.
                for (int r = 0; r < rows; r++) {
                    numbers[list][0][r] = r;
                }
            }
        }
    }

    public Target target() {
        return target;
    }

    public FunctionInput scalar(int slot, long value) {
        longs[slot] = value;
        return this;
    }

    public FunctionInput scalar(int slot, String value) {
        strings[slot] = value == null ? "" : value;
        return this;
    }

    public FunctionInput cell(int list, int row, int field, long value) {
        numbers[list][field][row] = value;
        return this;
    }

    public FunctionInput cell(int list, int row, int field, String value) {
        texts[list][field][row] = value == null ? "" : value;
        return this;
    }

    public int size(int list) {
        return sizes[list];
    }

    public long number(int list, int row, int field) {
        return numbers[list][field][row];
    }

    public String text(int list, int row, int field) {
        return texts[list][field][row];
    }

    /// The record as JSON, as a remote function is sent it.
    public String toJson() {
        StringBuilder b = new StringBuilder(256);
        b.append("{\"target\":");
        quote(b, target.name);
        b.append(",\"cart\":{");
        for (int iter = 0; iter < Target.SCALARS.length; iter++) {
            b.append(iter == 0 ? "" : ",");
            quote(b, Target.SCALARS[iter]);
            b.append(':');
            if (Target.scalarType(iter) == Target.STRING) {
                quote(b, strings[iter]);
            } else {
                b.append(longs[iter]);
            }
        }
        b.append('}');
        for (int list = 0; list < target.listCount(); list++) {
            b.append(',');
            quote(b, target.lists[list]);
            b.append(":[");
            for (int r = 0; r < sizes[list]; r++) {
                b.append(r == 0 ? "{" : ",{");
                for (int f = 0; f < target.fieldCount(list); f++) {
                    b.append(f == 0 ? "" : ",");
                    quote(b, target.fields[list][f]);
                    b.append(':');
                    if (target.fieldType(list, f) == Target.STRING) {
                        quote(b, texts[list][f][r]);
                    } else {
                        b.append(numbers[list][f][r]);
                    }
                }
                b.append('}');
            }
            b.append(']');
        }
        return b.append('}').toString();
    }

    static void quote(StringBuilder b, String s) {
        b.append('"');
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c == '"' || c == '\\') {
                b.append('\\').append(c);
            } else if (c < 0x20) {
                String hex = Integer.toHexString(c);
                b.append("\\u00").append(hex.length() < 2 ? "0" : "").append(hex);
            } else {
                b.append(c);
            }
        }
        b.append('"');
    }
}
