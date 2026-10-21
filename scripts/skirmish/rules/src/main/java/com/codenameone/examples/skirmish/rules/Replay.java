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
package com.codenameone.examples.skirmish.rules;

import java.util.ArrayList;
import java.util.List;

/// Everything needed to watch a match again: a seed, two decks and the commands.
///
/// No frames are recorded. A replay re-runs [Match] from these inputs, which is why
/// one is a few hundred bytes, and why a change to a card's numbers changes what a
/// replay shows (the format carries [#rulesVersion] so a viewer can say so).
public final class Replay {
    public static final String MAGIC = "SKR1";

    private final long seed;
    private final String[] deck0;
    private final String[] deck1;
    private final List<Command> commands;
    private final String rulesVersion;

    public Replay(long seed, String[] deck0, String[] deck1, List<Command> commands) {
        this(seed, deck0, deck1, commands, RulesVersion.ID);
    }

    public Replay(long seed, String[] deck0, String[] deck1, List<Command> commands, String rulesVersion) {
        this.seed = seed;
        this.deck0 = deck0;
        this.deck1 = deck1;
        this.commands = new ArrayList<Command>(commands);
        this.rulesVersion = rulesVersion;
    }

    public long seed() { return seed; }
    public String[] deck(int p) { return p == 0 ? deck0 : deck1; }
    public List<Command> commands() { return commands; }
    public String rulesVersion() { return rulesVersion; }

    /// A fresh match at tick 0.
    public Match start() {
        return new Match(seed, deck0, deck1);
    }

    /// The match run to its end.
    public Match play() {
        Match m = start();
        m.runToEnd(commands);
        return m;
    }

    /// A match advanced to `tick`, for scrubbing: re-simulating is cheap enough
    /// that a viewer seeks backwards by starting over.
    public Match seek(int tick) {
        Match m = start();
        while (m.tick() < tick && !m.isOver()) {
            m.step(commands);
        }
        return m;
    }

    /// `SKR1|rules|seed|deck0|deck1|cmd;cmd;...`
    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(MAGIC).append('|').append(rulesVersion).append('|').append(seed).append('|');
        join(sb, deck0);
        sb.append('|');
        join(sb, deck1);
        sb.append('|');
        for (int i = 0; i < commands.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(commands.get(i).encode());
        }
        return sb.toString();
    }

    private static void join(StringBuilder sb, String[] deck) {
        for (int i = 0; i < deck.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(deck[i]);
        }
    }

    public static Replay decode(String s) {
        String[] p = Text.split(s, '|');
        if (p.length != 6 || !MAGIC.equals(p[0])) {
            throw new IllegalArgumentException("not a replay");
        }
        List<Command> cmds = new ArrayList<Command>();
        if (p[5].length() > 0) {
            String[] c = Text.split(p[5], ';');
            for (int i = 0; i < c.length; i++) {
                cmds.add(Command.decode(c[i]));
            }
        }
        return new Replay(Long.parseLong(p[2]), Text.split(p[3], ','), Text.split(p[4], ','), cmds, p[1]);
    }
}
