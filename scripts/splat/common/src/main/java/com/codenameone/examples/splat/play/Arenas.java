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
package com.codenameone.examples.splat.play;

import com.codenameone.examples.splat.rules.Catalog;
import com.codenameone.examples.splat.rules.Mode;

/// The playlist: every field with every mode, as the `arena:MODE` references
/// the server takes. The fields ship with the app, so nothing is downloaded.
public final class Arenas {
    public static final String DEFAULT = "lot7:TEAM_PAINT";

    private Arenas() {
    }

    public static String[] all() {
        Mode[] modes = Mode.values();
        String[] out = new String[Catalog.ARENAS.length * modes.length];
        int k = 0;
        for (int a = 0; a < Catalog.ARENAS.length; a++) {
            for (int m = 0; m < modes.length; m++) {
                out[k++] = Catalog.ARENAS[a].id + ":" + modes[m].name();
            }
        }
        return out;
    }

    public static String arenaName(String ref) {
        return Catalog.arena(ref.substring(0, ref.indexOf(':'))).name;
    }

    public static Mode mode(String ref) {
        return Mode.byName(ref.substring(ref.indexOf(':') + 1));
    }
}
