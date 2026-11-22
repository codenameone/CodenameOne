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

/// Every marker and field the game ships with. Changing a number here changes the
/// game on the phone, the server and the replay viewer at once; bump
/// [RulesVersion#ID] when you do, because old replays will no longer agree.
public final class Catalog {
    // tag::splat-markers[]
    public static final MarkerDef[] MARKERS = {
        new MarkerDef("pump", "The Purist",
                "One shot, one paint. Owned exclusively by men who say 'back in my day'.",
                18, 2000, 0, 14, 70, 105),
        new MarkerDef("semi", "Tactical Office Supply",
                "Semi-automatic. Comes with a rail for a flashlight you will never attach.",
                6, 1700, 16, 40, 80, 100),
        new MarkerDef("ramp", "The Ramping Dad",
                "Fully automatic, fully unnecessary. Banned at three fields and one barbecue.",
                3, 1500, 44, 90, 105, 90),
    };
    // end::splat-markers[]

    public static final Arena[] ARENAS = {
        // tag::splat-arena[]
        // Every box is the footprint of the prop the app draws there (see
        // tools/pack-art.py): centre and size in decimetres, height last.
        new Arena.Builder("lot7", "Lot 7 Self Storage", 40, 60)
                .base(200, 110).hill(45)
                .spawn(140, 25).spawn(200, 20).spawn(260, 25).spawn(170, 45).spawn(230, 45)
                .spawn(200, 55)
                .mirror("sandbags", 130, 150, 34, 9, 13)
                .mirror("sandbags", 270, 150, 34, 9, 13)
                .mirror("sandbags-small", 95, 120, 25, 9, 10)
                .mirror("sandbags-small", 305, 120, 25, 9, 10)
                .mirror("container", 90, 175, 44, 21, 21)
                .mirror("container", 310, 195, 44, 21, 21)
                .mirror("barrier", 145, 235, 38, 7, 32)
                .mirror("car", 260, 245, 26, 55, 18)
                .mirror("tank", 335, 280, 24, 25, 21)
                .mirror("boxes", 55, 260, 13, 12, 22)
                .mirror("tires", 120, 295, 18, 11, 11)
                .mirror("barrier-single", 250, 175, 19, 7, 6)
                .mirror("snackbar", 200, 300, 22, 21, 21)
                .build(),
        // end::splat-arena[]
        new Arena.Builder("cul-de-sac", "Cul-de-Sac Siege (HOA Approved)", 36, 52)
                .base(180, 95).hill(40)
                .spawn(120, 20).spawn(180, 18).spawn(240, 20).spawn(150, 40).spawn(210, 40)
                .spawn(180, 50)
                .mirror("sofa", 180, 140, 34, 15, 13)
                .mirror("car", 85, 195, 26, 55, 18)
                .mirror("car", 285, 170, 55, 26, 18)
                .mirror("dumpster", 150, 230, 24, 13, 20)
                .mirror("tires", 250, 225, 18, 11, 11)
                .mirror("barrel", 235, 130, 8, 8, 10)
                .mirror("tree", 55, 250, 6, 6, 50)
                .mirror("watertower", 320, 255, 16, 24, 35)
                .mirror("crate", 120, 165, 8, 8, 8)
                .mirror("boxes", 245, 290, 13, 12, 22)
                .mirror("snackbar", 180, 260, 22, 21, 21)
                .build(),
    };

    private Catalog() {
    }

    public static MarkerDef marker(String id) {
        for (int i = 0; i < MARKERS.length; i++) {
            if (MARKERS[i].id.equals(id)) {
                return MARKERS[i];
            }
        }
        throw new IllegalArgumentException("marker " + id);
    }

    public static int markerIndex(String id) {
        for (int i = 0; i < MARKERS.length; i++) {
            if (MARKERS[i].id.equals(id)) {
                return i;
            }
        }
        throw new IllegalArgumentException("marker " + id);
    }

    public static Arena arena(String id) {
        for (int i = 0; i < ARENAS.length; i++) {
            if (ARENAS[i].id.equals(id)) {
                return ARENAS[i];
            }
        }
        Arena mission = Mission.registered(id);
        if (mission != null) {
            return mission;
        }
        throw new IllegalArgumentException("arena " + id);
    }
}
