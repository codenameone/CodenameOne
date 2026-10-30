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

import com.codenameone.examples.dash.rules.Catalog;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.Run;
import com.codenameone.examples.dash.rules.RulesVersion;

/// Verifies a submitted run. A client sends only its [Replay]; the server finds
/// the level the replay names, checks it is the same content, and plays the
/// inputs itself. The score on a leaderboard is the score of the server's run,
/// never a number the client reported.
public final class LevelRun {
    private LevelRun() {
    }

    /// The level id a replay of `levelId` in `pack` carries: the bare id for an
    /// official level, a [ContentRef] for anything else.
    public static String replayLevelId(ContentPack pack, String levelId) {
        if (Samples.OFFICIAL_ID.equals(pack.id)) {
            return levelId;
        }
        return new ContentRef(pack.id, pack.hash(), levelId).toString();
    }

    /// Plays `claimed` on `pack`'s level. Throws `IllegalArgumentException` when
    /// the replay was recorded under other rules, on other content or with more
    /// than one runner.
    public static Run verify(ContentPack pack, String levelId, Replay claimed) {
        if (!RulesVersion.ID.equals(claimed.rulesVersion())) {
            throw new IllegalArgumentException("recorded under other rules");
        }
        LevelDef level = pack.level(levelId);
        if (level == null) {
            throw new IllegalArgumentException("no level " + levelId);
        }
        if (level.contentHash() != claimed.levelHash()) {
            throw new IllegalArgumentException("the level changed since this run");
        }
        if (claimed.runners() != 1) {
            throw new IllegalArgumentException("a submitted run has one runner");
        }
        Catalog catalog = Samples.OFFICIAL_ID.equals(pack.id) ? Content.catalog() : pack.catalog();
        Run run = claimed.play(level, catalog);
        if (!run.over()) {
            throw new IllegalArgumentException("the run did not end");
        }
        return run;
    }
}
