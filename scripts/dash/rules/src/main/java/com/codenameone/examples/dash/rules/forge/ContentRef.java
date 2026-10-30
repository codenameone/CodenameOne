/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Perlevel is hereby granted, free of charge, to any person obtaining a copy
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
package com.codenameone.examples.dash.rules.forge;

/// The name of a level in a published pack version: `pack@hash#level`. A
/// replay and a level result carry it, so the server and a replay viewer load the
/// exact content the run was played on.
public final class ContentRef {
    public final String pack;
    public final String hash;
    public final String level;

    public ContentRef(String pack, String hash, String level) {
        this.pack = pack;
        this.hash = hash;
        this.level = level;
    }

    public static ContentRef parse(String s) {
        int at = s == null ? -1 : s.indexOf('@');
        int hashMark = s == null ? -1 : s.indexOf('#', at + 1);
        if (at <= 0 || hashMark <= at + 1 || hashMark == s.length() - 1) {
            throw new IllegalArgumentException("not a content reference");
        }
        return new ContentRef(s.substring(0, at), s.substring(at + 1, hashMark), s.substring(hashMark + 1));
    }

    @Override
    public String toString() {
        return pack + "@" + hash + "#" + level;
    }
}
