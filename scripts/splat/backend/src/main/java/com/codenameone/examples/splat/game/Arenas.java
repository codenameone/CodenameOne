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
package com.codenameone.examples.splat.game;

import com.codenameone.examples.splat.rules.Catalog;
import com.codenameone.examples.splat.rules.Mode;

/// A playlist reference: `arena:MODE`, such as `lot7:CAPTURE_THE_FLAG`. It is
/// what a client asks to play and what a match record stores as its level.
public final class Arenas {
    public static final String DEFAULT = "lot7:TEAM_PAINT";

    private Arenas() {
    }

    /// The canonical form of `ref`, or null when it names no arena and mode.
    /// A bare arena id plays Team Paint; null or empty plays the default.
    public static String normalize(String ref) {
        if (ref == null || ref.length() == 0) {
            return DEFAULT;
        }
        int colon = ref.indexOf(':');
        String arena = colon < 0 ? ref : ref.substring(0, colon);
        String mode = colon < 0 ? Mode.TEAM_PAINT.name() : ref.substring(colon + 1);
        try {
            Catalog.arena(arena);
            return arena + ":" + Mode.byName(mode).name();
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    public static String arena(String ref) {
        return ref.substring(0, ref.indexOf(':'));
    }

    public static Mode mode(String ref) {
        return Mode.byName(ref.substring(ref.indexOf(':') + 1));
    }
}
