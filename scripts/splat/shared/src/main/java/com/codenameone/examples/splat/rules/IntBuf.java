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
package com.codenameone.examples.splat.rules;

/// A growable list of ints: the form a match snapshot travels in. The wire and the
/// server's history both carry it as is, so it holds nothing but numbers.
public final class IntBuf {
    private int[] data = new int[256];
    private int size;

    public IntBuf add(int v) {
        if (size == data.length) {
            int[] n = new int[data.length * 2];
            System.arraycopy(data, 0, n, 0, size);
            data = n;
        }
        data[size++] = v;
        return this;
    }

    public IntBuf add(boolean v) {
        return add(v ? 1 : 0);
    }

    public IntBuf add(long v) {
        add((int) (v >>> 32));
        return add((int) v);
    }

    public int[] toArray() {
        int[] out = new int[size];
        System.arraycopy(data, 0, out, 0, size);
        return out;
    }

    /// Reads back what [IntBuf] wrote, in the same order.
    public static final class Reader {
        private final int[] data;
        private int pos;

        public Reader(int[] data) {
            this.data = data;
        }

        public int next() {
            if (pos >= data.length) {
                throw new IllegalArgumentException("snapshot truncated at " + pos);
            }
            return data[pos++];
        }

        public boolean bool() {
            return next() != 0;
        }

        public long nextLong() {
            long hi = next();
            long lo = next() & 0xffffffffL;
            return (hi << 32) | lo;
        }

        public boolean done() {
            return pos == data.length;
        }
    }
}
