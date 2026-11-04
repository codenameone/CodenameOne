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
package com.codenameone.examples.dash.rules.forge;

import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.Placement;
import com.codenameone.examples.dash.rules.Track;
import com.codenameone.examples.dash.rules.ViewMode;

/// The packs the game ships with: the official levels, and the blank pack the
/// editor starts a new one from.
public final class Samples {
    public static final String OFFICIAL_ID = "official";

    private Samples() {
    }

    public static ContentPack official() {
        ContentPack p = new ContentPack();
        p.id = OFFICIAL_ID;
        p.name = "Dash";
        p.author = "Dash team";
        p.description = "The levels the game ships with.";
        p.levels.addAll(Content.levels());
        return p;
    }

    /// A one-level pack that already plays: flat for a stretch, then a hurdle, a
    /// barrier and a coin line, with the camera starting side-on and swinging
    /// into the chase view so the author sees both at once.
    public static ContentPack blank(String id, String author) {
        ContentPack p = new ContentPack();
        p.id = id;
        p.name = "My levels";
        p.author = author;
        LevelDef d = new LevelDef("level-1", "Level 1");
        d.length = 300 * Track.UNIT;
        d.stars = new int[] {300, 330, 360};
        d.cue(0, ViewMode.SIDE_2D, 0);
        d.cue(150, ViewMode.CHASE_3D, 30);
        d.place(60, Content.HURDLE, Placement.ALL_LANES);
        d.place(110, Content.BARRIER, Placement.ALL_LANES);
        for (int i = 0; i < 6; i++) {
            d.place(200 + 3 * i, Content.COIN, 1);
        }
        d.place(240, Content.HAULER, 1);
        p.levels.add(d);
        return p;
    }
}
