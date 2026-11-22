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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.dash.rules.Bot;
import com.codenameone.examples.dash.rules.Clearance;
import com.codenameone.examples.dash.rules.ObstacleDef;
import com.codenameone.examples.dash.rules.Placement;
import com.codenameone.examples.dash.rules.Recorder;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.Run;
import org.junit.jupiter.api.Test;

class PackTest {
    @Test
    void theOfficialAndBlankPacksAreValidAndRoundTrip() {
        for (ContentPack p : new ContentPack[] {Samples.official(), Samples.blank("my-pack", "me")}) {
            assertFalse(Validator.hasErrors(Validator.check(p)), Validator.check(p).toString());
            String text = PackCodec.write(p);
            assertEquals(text, PackCodec.write(PackCodec.read(text)));
            assertEquals(p.hash(), p.copy().hash());
        }
    }

    @Test
    void customDefinitionsAndArtSurviveAndAreChecked() {
        ContentPack p = Samples.blank("art-pack", "me");
        p.defs.add(new ObstacleDef("u:me:log", Clearance.JUMP, 800, 500, false));
        p.art.put("u:me:log", "asset:abc");
        p.levels.get(0).place(150, "u:me:log", Placement.ALL_LANES);
        assertTrue(Validator.hasErrors(Validator.check(p)), "art without its file");
        AssetRef a = new AssetRef();
        a.sha256 = "abc";
        a.name = "log.glb";
        p.assets.add(a);
        assertFalse(Validator.hasErrors(Validator.check(p)), Validator.check(p).toString());
        ContentPack back = PackCodec.read(PackCodec.write(p));
        assertEquals("asset:abc", back.art.get("u:me:log"));
        assertEquals(p.hash(), back.hash());
    }

    @Test
    void aSubmittedRunIsReplayedOnTheServersOwnCopy() {
        ContentPack p = Samples.blank("run-pack", "me");
        Run run = new Run(p.level("level-1"), p.catalog(), 5, 1);
        Recorder rec = new Recorder(run);
        Bot bot = new Bot(5, 100);
        while (!run.over()) {
            rec.step(new int[] {bot.decide(run, 0)});
        }
        Replay replay = Replay.decode(rec.replay().encode());
        Run verified = LevelRun.verify(p, "level-1", replay);
        assertEquals(run.stateHash(), verified.stateHash());
        assertEquals(run.runner(0).score(), verified.runner(0).score());
        p.levels.get(0).place(250, "hurdle", 0);
        assertThrows(IllegalArgumentException.class, () -> LevelRun.verify(p, "level-1", replay));
    }
}
