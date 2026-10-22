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
package com.codenameone.examples.skirmish.battle;

import com.codenameone.examples.skirmish.rules.Command;
import com.codenameone.examples.skirmish.rules.forge.ContentPack;
import com.codenameone.examples.skirmish.rules.forge.DialogStep;
import com.codenameone.examples.skirmish.rules.forge.MissionDef;
import com.codenameone.examples.skirmish.rules.forge.MissionRun;
import java.util.ArrayList;
import java.util.List;

/// A Forge mission: the match its pack describes, its scripted waves and its
/// bot, and its story told in speech bubbles that can stop the clock.
///
/// [MissionRun] is the whole simulation, the class the server replays a result
/// with, so a mission played here and checked there is the same match.
public final class MissionDriver extends Driver {
    /// Shown a line of the mission's story.
    public interface Listener {
        void say(DialogStep step);
    }

    private final MissionRun run;
    private final ContentPack pack;
    private final List<DialogStep> dialog;
    private int nextLine;
    private DialogStep holding;
    private Listener listener;

    public MissionDriver(ContentPack pack, String missionId, long seed, String[] ownDeck) {
        this.pack = pack;
        this.run = new MissionRun(pack, missionId, seed, ownDeck);
        this.match = run.match();
        this.me = 0;
        dialog = new ArrayList<DialogStep>(run.mission().dialog);
        // Stable: two lines on one tick are said in the order they were written.
        for (int i = 1; i < dialog.size(); i++) {
            for (int j = i; j > 0 && dialog.get(j - 1).tick > dialog.get(j).tick; j--) {
                DialogStep t = dialog.get(j);
                dialog.set(j, dialog.get(j - 1));
                dialog.set(j - 1, t);
            }
        }
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public ContentPack pack() {
        return pack;
    }

    public MissionDef mission() {
        return run.mission();
    }

    public MissionRun run() {
        return run;
    }

    @Override
    public boolean canStep() {
        if (match.isOver() || holding != null) {
            return false;
        }
        while (nextLine < dialog.size() && dialog.get(nextLine).tick <= match.tick()) {
            DialogStep s = dialog.get(nextLine++);
            if (s.freeze) {
                holding = s;
            }
            if (listener != null) {
                listener.say(s);
            }
            if (holding != null) {
                return false;
            }
        }
        return true;
    }

    /// The player has read the line that stopped the clock.
    public void resume() {
        holding = null;
    }

    @Override
    public void step() {
        run.step();
    }

    @Override
    public void play(String card, int x, int y) {
        if (holding == null && match.canPlay(0, card, x, y)) {
            run.play(new Command(match.tick() + 1, 0, card, x, y));
        }
    }

    @Override
    public String name(int seat) {
        return seat == 0 ? "You" : run.mission().name;
    }
}
