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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// Holds the simulation still. Each line is a scripted match and the hash it must
/// end on; a change to the rules that moves any of them must bump
/// [RulesVersion#ID], then regenerate with `-Dsplat.regolden=true`.
class GoldenReplayTest {
    private static final String FILE = "golden-replays.txt";

    /// A human who walks forward, turns and fires on a schedule, among bots, so the
    /// corpus exercises recorded inputs as well as bot decisions.
    static Replay scripted(long seed, String arena, Mode mode) {
        List<Slot> slots = new ArrayList<Slot>();
        slots.add(new Slot("Scripted Human", 0, "semi", 0));
        for (int i = 1; i < 8; i++) {
            slots.add(new Slot("Bot " + i, i % 2, Catalog.MARKERS[i % 3].id, 30 + i * 9));
        }
        Replay r = new Replay(seed, arena, mode, slots);
        Match m = r.newMatch();
        Input[] frame = new Input[slots.size()];
        while (!m.over()) {
            int t = m.tick();
            int phase = (t / 45) % 4;
            int yaw = Trig.wrap(t * 9);
            int buttons = (phase == 1 ? Input.FIRE : 0) | (phase == 2 ? Input.SPRINT : 0)
                    | (t % 300 == 0 ? Input.JUMP : 0) | (t % 900 == 450 ? Input.THROW : 0)
                    | (t % 97 == 0 ? Input.WIPE : 0);
            frame[0] = new Input(phase == 3 ? 90 : 0, phase == 0 ? 0 : 127, yaw, (t % 200) - 100, buttons, t - 3);
            r.record(t, frame);
            m.step(frame);
        }
        return r;
    }

    static String corpus() {
        StringBuilder sb = new StringBuilder();
        long[] seeds = {1, 2, 77, 1234567};
        for (Arena a : Catalog.ARENAS) {
            for (Mode mode : Mode.values()) {
                for (long seed : seeds) {
                    Replay r = scripted(seed, a.id, mode);
                    Match m = Replay.decode(r.encode()).play();
                    sb.append(a.id).append(' ').append(mode.name()).append(' ').append(seed).append(' ')
                            .append(m.tick()).append(' ').append(m.score(0)).append(':').append(m.score(1))
                            .append(' ').append(Long.toHexString(m.stateHash())).append('\n');
                }
            }
        }
        return "rules " + RulesVersion.ID + "\n" + sb;
    }

    @Test
    void corpusIsUnchanged() throws Exception {
        String actual = corpus();
        if (Boolean.getBoolean("splat.regolden")) {
            File f = new File("src/test/resources/" + FILE);
            try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
                w.write(actual);
            }
            return;
        }
        String expected;
        try (InputStream in = GoldenReplayTest.class.getResourceAsStream("/" + FILE)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
            expected = new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
        assertEquals(expected, actual);
    }

    @Test
    void decodedReplayMatchesTheRecordedRun() {
        Replay r = scripted(9, "weekend", Mode.SNACK_BAR);
        Match direct = r.play();
        Match decoded = Replay.decode(r.encode()).play();
        assertEquals(direct.stateHash(), decoded.stateHash());
    }
}
