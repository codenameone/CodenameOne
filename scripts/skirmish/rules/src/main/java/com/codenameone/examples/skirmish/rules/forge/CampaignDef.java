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
package com.codenameone.examples.skirmish.rules.forge;

import java.util.ArrayList;
import java.util.List;

/// Missions played in order. Each one opens when the one before it has been won,
/// and the story is told between them.
public final class CampaignDef {
    public String id = "";
    public String name = "";
    /// Shown before the first mission.
    public String intro = "";
    /// Shown after the last one is won.
    public String outro = "";
    /// Mission ids of the same pack, in the order they are played.
    public List<String> missions = new ArrayList<String>();
    /// Stars needed in total before the last mission opens, 0 for none.
    public int finaleStars;

    /// Whether mission `index` is open, given the stars won on each mission so far.
    public boolean isOpen(int index, int[] starsByMission) {
        if (index == 0) {
            return true;
        }
        if (index < 0 || index >= missions.size() || starsByMission[index - 1] <= 0) {
            return false;
        }
        if (index == missions.size() - 1 && finaleStars > 0) {
            int total = 0;
            for (int i = 0; i < index; i++) {
                total += starsByMission[i];
            }
            return total >= finaleStars;
        }
        return true;
    }
}
