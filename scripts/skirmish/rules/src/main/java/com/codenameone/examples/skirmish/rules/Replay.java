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
///
/// A battle on Forge content also names that content -- the pack, its hash and
/// the mission -- in [#content]. The replay cannot carry the rules themselves, so
/// whoever plays it back looks the pack up and passes its rules to [#withRules].
public final class Replay {
    public static final String MAGIC = "SKR1";
    /// The format of a replay that names custom content; ranked replays stay SKR1.
    public static final String MAGIC_CONTENT = "SKR2";
    private static final String NO_DECK = "-";

    private final long seed;
    private final String[] deck0;
    private final String[] deck1;
    private final List<Command> commands;
    private final String rulesVersion;
    private final String content;
    private final MatchRules rules;

    public Replay(long seed, String[] deck0, String[] deck1, List<Command> commands) {
        this(seed, deck0, deck1, commands, RulesVersion.ID);
    }

    public Replay(long seed, String[] deck0, String[] deck1, List<Command> commands, String rulesVersion) {
        this(seed, deck0, deck1, commands, rulesVersion, "", MatchRules.CLASSIC);
    }

    /// A replay of a battle on custom content. `content` is the reference that
    /// [com.codenameone.examples.skirmish.rules.forge.ContentRef] writes, and
    /// `rules` the rules it resolves to.
    public Replay(long seed, String[] deck0, String[] deck1, List<Command> commands, String rulesVersion,
            String content, MatchRules rules) {
        this.content = content == null ? "" : content;
        this.rules = rules;
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
    /// Empty for a ranked battle.
    public String content() { return content; }
    public MatchRules rules() { return rules; }

    /// This replay played back under `rules`, which the caller resolved from [#content].
    public Replay withRules(MatchRules r) {
        return new Replay(seed, deck0, deck1, commands, rulesVersion, content, r);
    }

    /// A fresh match at tick 0.
    public Match start() {
        return new Match(seed, deck0, deck1, rules);
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
        sb.append(content.length() == 0 ? MAGIC : MAGIC_CONTENT).append('|').append(rulesVersion)
                .append('|').append(seed).append('|');
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
        if (content.length() > 0) {
            sb.append('|').append(content);
        }
        return sb.toString();
    }

    private static void join(StringBuilder sb, String[] deck) {
        if (deck == null) {
            // A mission side with no deck; it plays only scripted waves.
            sb.append(NO_DECK);
            return;
        }
        for (int i = 0; i < deck.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(deck[i]);
        }
    }

    private static String[] deck(String field) {
        return NO_DECK.equals(field) ? null : Text.split(field, ',');
    }

    /// Reads a replay. One that names custom content comes back with classic rules;
    /// resolve [#content] and call [#withRules] before playing it.
    public static Replay decode(String s) {
        String[] p = Text.split(s, '|');
        boolean classic = p.length == 6 && MAGIC.equals(p[0]);
        boolean custom = p.length == 7 && MAGIC_CONTENT.equals(p[0]);
        if (!classic && !custom) {
            throw new IllegalArgumentException("not a replay");
        }
        List<Command> cmds = new ArrayList<Command>();
        if (p[5].length() > 0) {
            String[] c = Text.split(p[5], ';');
            for (int i = 0; i < c.length; i++) {
                cmds.add(Command.decode(c[i]));
            }
        }
        return new Replay(Long.parseLong(p[2]), deck(p[3]), deck(p[4]), cmds, p[1],
                custom ? p[6] : "", MatchRules.CLASSIC);
    }
}
