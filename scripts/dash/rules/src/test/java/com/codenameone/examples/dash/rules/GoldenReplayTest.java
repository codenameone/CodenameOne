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
package com.codenameone.examples.dash.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// Pins the outcome of a fixed set of replays over every official level.
///
/// The same file is the corpus the cross-platform determinism check plays on
/// ParparVM and in the browser: every runtime must reach these exact hashes.
///
/// A deliberate rules change makes this fail. Bump [RulesVersion#ID] and rewrite
/// the file with `mvn test -Ddash.regolden=true`.
class GoldenReplayTest {
    static final String FILE = "src/test/resources/golden-replays.txt";

    @Test
    void goldenReplaysStillPlayOutTheSame() throws Exception {
        if (Boolean.getBoolean("dash.regolden")) {
            regenerate();
        }
        List<String> lines = read();
        assertTrue(lines.size() >= 9, "corpus");
        for (String line : lines) {
            int bar = line.indexOf(' ');
            long expected = Text.parseHex(line.substring(0, bar));
            Replay r = Replay.decode(line.substring(bar + 1));
            assertEquals(RulesVersion.ID, r.rulesVersion(), "replay recorded under other rules");
            LevelDef level = Content.level(r.levelId());
            assertEquals(Long.toHexString(level.contentHash()), Long.toHexString(r.levelHash()), r.levelId());
            assertEquals(Long.toHexString(expected), Long.toHexString(r.play(level, Content.catalog()).stateHash()),
                    r.levelId() + " seed " + r.seed());
        }
    }

    private static List<String> read() throws Exception {
        List<String> out = new ArrayList<String>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                GoldenReplayTest.class.getResourceAsStream("/golden-replays.txt"), StandardCharsets.UTF_8))) {
            String l;
            while ((l = in.readLine()) != null) {
                if (l.length() > 0 && l.charAt(0) != '#') {
                    out.add(l);
                }
            }
        }
        return out;
    }

    private static void regenerate() throws Exception {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(FILE)), StandardCharsets.UTF_8)) {
            w.write("# <final state hash> <replay>; regenerate with -Ddash.regolden=true\n");
            for (LevelDef level : Content.levels()) {
                for (long seed = 1000; seed < 1003; seed++) {
                    Replay r = RunTest.botRace(level, seed, (int) (seed - 998));
                    w.write(Long.toHexString(r.play(level, Content.catalog()).stateHash()) + " " + r.encode() + "\n");
                }
            }
        }
        throw new IllegalStateException("golden file rewritten; re-run without -Ddash.regolden");
    }
}
