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
package com.codenameone.examples.dash.run;

/// The colours a runner wears: a suit, a glowing trim (the visor, the shoe
/// soles and the stripes) and a dark base for the gloves and the helmet shell.
///
/// The run draws every runner from the same procedural body ([Rig]) and the
/// menus paint the same body flat ([com.codenameone.examples.dash.ui.Portraits]),
/// so a runner the shop sells is nothing but a row here. Add one by adding a
/// catalog item on the server and a case to [#of].
public final class Outfit {
    public final String id;
    public final int suit;
    public final int trim;
    public final int shell;

    private Outfit(String id, int suit, int trim, int shell) {
        this.id = id;
        this.suit = suit;
        this.trim = trim;
        this.shell = shell;
    }

    /// The outfits bots and unknown runners wear, in turn.
    private static final Outfit[] CROWD = {
        new Outfit("crowd-slate", 0xff5d6b86, 0xffffb547, 0xff20263a),
        new Outfit("crowd-moss", 0xff4f7a5e, 0xfff4f1e8, 0xff1d2a24),
        new Outfit("crowd-plum", 0xff7a4f78, 0xff7dffd6, 0xff2a1d2a),
        new Outfit("crowd-sand", 0xffb59a6d, 0xff3fd0ff, 0xff3a3022),
    };

    // tag::dash-outfits[]
    /// The outfit for a shop runner id; `fallback` picks one for a bot.
    public static Outfit of(String runnerId, int fallback) {
        if ("runner-nova".equals(runnerId)) {
            return new Outfit(runnerId, 0xff6c4dff, 0xffc8ff3d, 0xff1a1440);
        }
        if ("runner-kite".equals(runnerId)) {
            return new Outfit(runnerId, 0xff0fb5a8, 0xffffe14d, 0xff0b2f33);
        }
        if ("runner-dash".equals(runnerId) || runnerId == null && fallback == 0) {
            return new Outfit("runner-dash", 0xffff5a3c, 0xff35e8ff, 0xff241326);
        }
        return CROWD[Math.abs(fallback) % CROWD.length];
    }
    // end::dash-outfits[]

    /// This outfit faded towards grey, for a ghost from an earlier run.
    public Outfit ghost() {
        return new Outfit(id, mix(suit, 0xff9aa4b8, 0.65f), mix(trim, 0xffffffff, 0.5f), mix(shell, 0xff9aa4b8, 0.6f));
    }

    static int mix(int a, int b, float k) {
        int r = (int) (((a >> 16) & 0xff) * (1 - k) + ((b >> 16) & 0xff) * k);
        int g = (int) (((a >> 8) & 0xff) * (1 - k) + ((b >> 8) & 0xff) * k);
        int bl = (int) ((a & 0xff) * (1 - k) + (b & 0xff) * k);
        return 0xff000000 | (r << 16) | (g << 8) | bl;
    }
}
