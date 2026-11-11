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
import com.codenameone.examples.splat.rules.Input;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.rules.Mode;
import com.codenameone.examples.splat.rules.Slot;
import java.util.ArrayList;
import java.util.List;

/// Offline against the rules' own bots: no server, no account, works on a plane.
/// The bots are the ones the server fills a quick match with, so practice is
/// honest about what online feels like.
public final class PracticeDriver extends Driver {
    /// The weekend league's regulars. Original characters, all of them sure they
    /// were not hit.
    static final String[] BOTS = {
        "Gary (Tactical)", "Kevin From Accounts", "Sgt. Barbecue", "Linda, Team Mom",
        "Big Doug", "The Intern", "Coach Pete", "Uncle Ray", "Brenda (Ref In Training)",
        "Dale, Still Arguing", "Todd With A Drone", "Marcy Ninja",
    };
    private static final long BRIEFING_MILLIS = 3000;

    private final Input[] frame;
    private final long startsAt = System.currentTimeMillis() + BRIEFING_MILLIS;

    public PracticeDriver(String arena, Mode mode, String marker, int players, int skill, long seed) {
        List<Slot> slots = new ArrayList<Slot>();
        names = new String[players];
        for (int i = 0; i < players; i++) {
            String name = i == 0 ? "You" : BOTS[(int) (((seed & 0xffffff) + i) % BOTS.length)];
            String m = i == 0 ? marker : Catalog.MARKERS[(int) (((seed & 0xffffff) / 7 + i) % Catalog.MARKERS.length)].id;
            // Your own team is a notch worse, so carrying it is the job.
            int s = i == 0 ? 0 : i % 2 == 0 ? Math.max(10, skill - 10) : skill;
            slots.add(new Slot(name, i % 2, m, s));
            names[i] = name;
        }
        match = new Match(seed, Catalog.arena(arena), mode, slots);
        frame = new Input[players];
        me = 0;
    }

    @Override
    public boolean canStep() {
        return countdownTicks() == 0 && !match.over();
    }

    @Override
    public void step(Input in) {
        frame[0] = in;
        match.step(frame);
        emit(match.events());
    }

    @Override
    public int countdownTicks() {
        long left = startsAt - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) (left * Match.TICK_HZ / 1000) + 1;
    }
}
