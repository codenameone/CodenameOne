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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MissionTest {
    @Test
    void roundTripsThroughJsonAndMirrors() {
        Mission m = Mission.blank();
        m.code = "abc123";
        m.mode = Mode.CAPTURE_THE_FLAG;
        m.props.add(new Mission.Prop("car", 100, 200, true));
        assertNull(m.problem());
        Mission back = Mission.fromJson(m.toJson());
        assertEquals(m.toJson(), back.toJson());
        Arena a = back.arena();
        assertEquals(m.props.size() * 2, a.boxes().size());
        assertEquals(a.width - a.spawnX(0, 0), a.spawnX(1, 0));
    }

    @Test
    void registeredMissionPlaysAsAnArena() {
        Mission m = Mission.blank();
        m.code = "play01";
        Arena a = m.register();
        assertSame(a, Catalog.arena("m-play01"));
        List<Slot> slots = new ArrayList<Slot>();
        for (int i = 0; i < 4; i++) {
            slots.add(new Slot("B" + i, i % 2, "semi", 50));
        }
        Match match = new Match(7, a, m.mode, slots);
        Rng rng = new Rng(3);
        for (int t = 0; t < 300; t++) {
            Input[] in = new Input[match.playerCount()];
            for (int p = 0; p < in.length; p++) {
                in[p] = Bot.think(match, match.player(p), rng);
            }
            match.step(in);
        }
        assertTrue(match.tick() >= 300);
    }

    @Test
    void refusesWhatCannotBePlayed() {
        Mission m = Mission.blank();
        m.spawns.clear();
        assertNotNull(m.problem());
        m = Mission.blank();
        m.props.add(new Mission.Prop("car", 200, 270, false));
        assertNotNull(m.problem(), "a prop over the half line");
        m = Mission.blank();
        m.spawns.add(new int[] {200, 130});
        assertNotNull(m.problem(), "a spawn inside the sandbags");
        assertThrows(IllegalArgumentException.class, () -> Mission.fromJson("[1]"));
        assertThrows(IllegalArgumentException.class, () -> Mission.fromJson("{\"mode\":\"NOPE\"}"));
    }
}
