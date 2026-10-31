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
package com.codenameone.examples.dash.rules;

/// 64-bit FNV-1a, fed integers and strings. Used for state hashes and level
/// content hashes; both must agree byte for byte on every runtime, so strings are
/// hashed by `char`, never through an encoding.
public final class Hash {
    private long h = 0xcbf29ce484222325L;

    public Hash add(long v) {
        for (int i = 0; i < 8; i++) {
            h ^= (v >>> (i * 8)) & 0xff;
            h *= 0x100000001b3L;
        }
        return this;
    }

    public Hash add(boolean b) {
        return add(b ? 1 : 0);
    }

    public Hash add(String s) {
        if (s == null) {
            return add(-1);
        }
        add(s.length());
        for (int i = 0; i < s.length(); i++) {
            add(s.charAt(i));
        }
        return this;
    }

    public long value() {
        return h;
    }
}
