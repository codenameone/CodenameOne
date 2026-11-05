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

    public static final Arena[] ARENAS = {
        new Arena.Builder("weekend", "Weekend Warrior Paintball Park", 40, 60)
                .base(200, 110).hill(45)
                .spawn(140, 25).spawn(200, 20).spawn(260, 25).spawn(170, 45).spawn(230, 45)
                .spawn(200, 55)
                .mirror("bunker", 120, 120, 30, 12, 13)
                .mirror("bunker", 280, 120, 30, 12, 13)
                .mirror("haybale", 200, 140, 24, 10, 8)
                .mirror("dorito", 80, 200, 14, 14, 15)
                .mirror("dorito", 320, 200, 14, 14, 15)
                .mirror("minivan", 200, 230, 45, 20, 17)
                .mirror("crate", 60, 260, 10, 10, 5)
                .mirror("crate", 340, 265, 10, 10, 5)
                .mirror("haybale", 140, 280, 20, 9, 8)
                .mirror("haybale", 260, 280, 20, 9, 8)
                .mirror("snackbar", 200, 300, 30, 12, 24)
                .build(),
        new Arena.Builder("cul-de-sac", "Cul-de-Sac Siege (HOA Approved)", 36, 52)
                .base(180, 95).hill(40)
                .spawn(120, 20).spawn(180, 18).spawn(240, 20).spawn(150, 40).spawn(210, 40)
                .spawn(180, 50)
                .mirror("hedge", 90, 110, 60, 8, 12)
                .mirror("hedge", 270, 110, 60, 8, 12)
                .mirror("bbq", 180, 120, 12, 8, 11)
                .mirror("trampoline", 70, 190, 30, 30, 6)
                .mirror("shed", 300, 190, 30, 26, 25)
                .mirror("recycling", 170, 200, 8, 8, 11)
                .mirror("minivan", 120, 240, 20, 45, 17)
                .mirror("mailbox", 260, 240, 4, 4, 13)
                .mirror("fountain", 180, 260, 26, 26, 9)
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
        throw new IllegalArgumentException("arena " + id);
    }
}
