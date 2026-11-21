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
package com.codenameone.examples.brickyard.studio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// An experimental visual behavior for one part: a Touched trigger followed
/// by a chain of steps, compiled to plain Luau.
///
/// The graph never runs by itself. [#toLuau()] writes an ordinary script
/// that goes through the same sandboxed interpreter as any hand-written
/// one, and the graph rides along in that script's first line, a Luau
/// comment, so a place saved with it still plays in a player that knows
/// nothing about graphs. The editor reads the graph back with
/// [#fromSource(String)], which answers null for anything it did not write
/// verbatim: a malformed header, a header with no valid steps, or a script
/// the creator edited by hand. A null means "show the text editor", never
/// "overwrite the text".
public final class LogicGraph {
    /// The prefix of the script's first line that carries the graph.
    public static final String HEADER = "--[[brickyard-logic v1 ";
    /// The name the studio gives the script it writes into a part.
    public static final String SCRIPT_NAME = "Logic";

    /// Only a character's touch continues; also finds the player.
    public static final String IS_PLAYER = "player";
    /// Adds an amount to a leaderstat value, creating it when missing.
    public static final String ADD_STAT = "stat";
    /// Flashes the part.
    public static final String FLASH = "flash";
    /// Removes the part, so the touch happens once.
    public static final String DESTROY = "destroy";

    private static final int MAX_STEPS = 8;
    private static final int MAX_NAME = 20;
    private static final int MAX_AMOUNT = 1000;

    private final List<Step> steps = new ArrayList<Step>();

    /// One node after the trigger.
    public static final class Step {
        private final String kind;
        private final String stat;
        private final int amount;

        Step(String kind, String stat, int amount) {
            this.kind = kind;
            this.stat = stat;
            this.amount = amount;
        }

        /// One of [#IS_PLAYER], [#ADD_STAT], [#FLASH] or [#DESTROY].
        public String kind() {
            return kind;
        }

        /// The leaderstat an [#ADD_STAT] step adds to.
        public String stat() {
            return stat;
        }

        /// What an [#ADD_STAT] step adds.
        public int amount() {
            return amount;
        }

        /// The label the node shows.
        public String label() {
            if (ADD_STAT.equals(kind)) {
                return "Add " + amount + " " + stat;
            }
            if (IS_PLAYER.equals(kind)) {
                return "Is a player?";
            }
            return FLASH.equals(kind) ? "Flash part" : "Destroy part";
        }
    }

    /// The award-a-collectible behavior a new Logic tab starts from.
    public static LogicGraph collectible() {
        LogicGraph g = new LogicGraph();
        g.add(IS_PLAYER);
        g.addStat("Coins", 1);
        g.add(FLASH);
        g.add(DESTROY);
        return g;
    }

    /// The steps after the trigger, in order.
    public List<Step> steps() {
        return Collections.unmodifiableList(steps);
    }

    /// Appends a step of `kind` with default arguments; false when the chain
    /// is full or the kind is unknown.
    public boolean add(String kind) {
        if (ADD_STAT.equals(kind)) {
            return addStat("Coins", 1);
        }
        if (!IS_PLAYER.equals(kind) && !FLASH.equals(kind) && !DESTROY.equals(kind)) {
            return false;
        }
        if (steps.size() >= MAX_STEPS) {
            return false;
        }
        steps.add(new Step(kind, null, 0));
        return true;
    }

    /// Appends an add-to-leaderstat step; false when the name or amount is
    /// out of range or the chain is full.
    public boolean addStat(String stat, int amount) {
        if (steps.size() >= MAX_STEPS || !validName(stat) || amount < 1 || amount > MAX_AMOUNT) {
            return false;
        }
        steps.add(new Step(ADD_STAT, stat, amount));
        return true;
    }

    /// Removes the last step, if any.
    public void removeLast() {
        if (!steps.isEmpty()) {
            steps.remove(steps.size() - 1);
        }
    }

    /// A leaderstat name is letters, digits and spaces: it is written into a
    /// Luau string literal, so nothing that could end one is allowed in.
    static boolean validName(String s) {
        if (s == null || s.length() == 0 || s.length() > MAX_NAME) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /// The graph as one line, `touched|player|stat:Coins:1|flash`.
    public String serialize() {
        StringBuilder sb = new StringBuilder("touched");
        for (Step s : steps) {
            sb.append('|').append(s.kind);
            if (ADD_STAT.equals(s.kind)) {
                sb.append(':').append(s.stat).append(':').append(s.amount);
            }
        }
        return sb.toString();
    }

    /// Parses [#serialize()]'s output; null for anything malformed.
    public static LogicGraph parse(String text) {
        if (text == null || text.length() > 400 || !text.startsWith("touched")) {
            return null;
        }
        LogicGraph g = new LogicGraph();
        String rest = text.substring("touched".length());
        while (rest.length() > 0) {
            if (rest.charAt(0) != '|') {
                return null;
            }
            int next = rest.indexOf('|', 1);
            String token = next < 0 ? rest.substring(1) : rest.substring(1, next);
            rest = next < 0 ? "" : rest.substring(next);
            if (token.startsWith(ADD_STAT + ":")) {
                int colon = token.lastIndexOf(':');
                if (colon <= ADD_STAT.length()) {
                    return null;
                }
                int amount;
                try {
                    amount = Integer.parseInt(token.substring(colon + 1));
                } catch (NumberFormatException err) {
                    return null;
                }
                if (!g.addStat(token.substring(ADD_STAT.length() + 1, colon), amount)) {
                    return null;
                }
            } else if (ADD_STAT.equals(token) || !g.add(token)) {
                return null;
            }
        }
        return g;
    }

    /// The graph a script's source carries, or null when the source is not
    /// exactly what that graph compiles to -- so a hand edit detaches it.
    public static LogicGraph fromSource(String source) {
        if (source == null || !source.startsWith(HEADER)) {
            return null;
        }
        int end = source.indexOf("]]", HEADER.length());
        if (end < 0) {
            return null;
        }
        LogicGraph g = parse(source.substring(HEADER.length(), end));
        if (g == null || !g.toLuau().equals(source)) {
            return null;
        }
        return g;
    }

    /// True when `source` starts with a graph header but no longer matches
    /// it: a generated script the creator edited by hand.
    public static boolean isDetached(String source) {
        return source != null && source.startsWith(HEADER) && fromSource(source) == null;
    }

    private boolean has(String kind) {
        for (Step s : steps) {
            if (kind.equals(s.kind)) {
                return true;
            }
        }
        return false;
    }

    /// The script this graph compiles to. A step that needs a player (adding
    /// to a leaderstat) finds one even without an explicit "Is a player?"
    /// node, and a touch that destroys the part fires once.
    public String toLuau() {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append(serialize()).append("]]\n");
        sb.append("-- Generated by the Brickyard Logic tab. Edit the graph there;\n");
        sb.append("-- editing this text detaches the graph and keeps your edits.\n");
        sb.append("local part = script.Parent\n");
        sb.append("local busy = false\n");
        sb.append("part.Touched:Connect(function(hit)\n");
        sb.append("    if busy then return end\n");
        boolean player = false;
        boolean once = has(DESTROY);
        boolean opened = false;
        for (Step s : steps) {
            boolean needsPlayer = IS_PLAYER.equals(s.kind) || ADD_STAT.equals(s.kind);
            if (needsPlayer && !player) {
                sb.append("    local player = game.Players:GetPlayerFromCharacter(hit.Parent)\n");
                sb.append("    if not player then return end\n");
                player = true;
            }
            if (!opened) {
                sb.append("    busy = true\n");
                opened = true;
            }
            if (ADD_STAT.equals(s.kind)) {
                sb.append("    do\n");
                sb.append("        local stats = player:FindFirstChild(\"leaderstats\")\n");
                sb.append("        if not stats then\n");
                sb.append("            stats = Instance.new(\"Folder\")\n");
                sb.append("            stats.Name = \"leaderstats\"\n");
                sb.append("            stats.Parent = player\n");
                sb.append("        end\n");
                sb.append("        local value = stats:FindFirstChild(\"").append(s.stat).append("\")\n");
                sb.append("        if not value then\n");
                sb.append("            value = Instance.new(\"IntValue\")\n");
                sb.append("            value.Name = \"").append(s.stat).append("\"\n");
                sb.append("            value.Parent = stats\n");
                sb.append("        end\n");
                sb.append("        value.Value += ").append(s.amount).append('\n');
                sb.append("    end\n");
            } else if (FLASH.equals(s.kind)) {
                sb.append("    part.Transparency = 0.6\n");
                if (!once) {
                    sb.append("    task.wait(0.3)\n");
                    sb.append("    part.Transparency = 0\n");
                }
            } else if (DESTROY.equals(s.kind)) {
                sb.append("    part:Destroy()\n");
            }
        }
        if (!opened) {
            sb.append("    busy = true\n");
        }
        if (!once) {
            sb.append("    task.wait(0.5)\n");
            sb.append("    busy = false\n");
        }
        sb.append("end)\n");
        return sb.toString();
    }
}
