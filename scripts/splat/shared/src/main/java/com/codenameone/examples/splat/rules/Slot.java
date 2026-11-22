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

/// One place on the roster, fixed for the whole match. A bot's inputs are made
/// inside [Match#step], so a replay never records them.
public final class Slot {
    public final String name;
    public final int team;
    public final String marker;
    /// 0 for a human; otherwise the bot's skill, 1 (Uncle Dave) to 100.
    public final int botSkill;

    public Slot(String name, int team, String marker, int botSkill) {
        if (team != 0 && team != 1) {
            throw new IllegalArgumentException("team " + team);
        }
        Catalog.marker(marker);
        this.name = name;
        this.team = team;
        this.marker = marker;
        this.botSkill = FixedMath.clamp(botSkill, 0, 100);
    }

    public boolean bot() {
        return botSkill > 0;
    }

    /// `team marker skill name`; the name is last because it may hold spaces.
    public String encode() {
        return team + " " + marker + " " + botSkill + " " + name;
    }

    public static Slot decode(String s) {
        int a = s.indexOf(' ');
        int b = s.indexOf(' ', a + 1);
        int c = s.indexOf(' ', b + 1);
        return new Slot(s.substring(c + 1), Integer.parseInt(s.substring(0, a)), s.substring(a + 1, b),
                Integer.parseInt(s.substring(b + 1, c)));
    }
}
