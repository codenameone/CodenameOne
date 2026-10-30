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
package com.codenameone.examples.dash;

import com.codenameone.examples.dash.rules.Bot;
import com.codenameone.examples.dash.rules.Catalog;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.Recorder;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.Run;

/// Solo runs the way the app records them: a bot plays, a [Recorder] keeps
/// every input.
public final class Runs {
    private Runs() {
    }

    public static Replay bot(LevelDef level, Catalog catalog, long seed, int skill) {
        Recorder rec = new Recorder(new Run(level, catalog, seed, 1));
        Bot bot = new Bot(seed, skill);
        while (!rec.run().over()) {
            rec.step(new int[] {bot.decide(rec.run(), 0)});
        }
        return rec.replay();
    }
}
