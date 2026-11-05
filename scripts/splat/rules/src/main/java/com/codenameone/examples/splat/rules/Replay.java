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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A whole match as data: the seed, the field, the mode, the roster and every
/// human input that changed. Nothing is recorded per frame; the viewer runs the
/// match again, which is why a replay of a six-minute 6v6 is a few kilobytes.
public final class Replay {
    public static final String MAGIC = "splat-replay";

    public final long seed;
    public final String arena;
    public final Mode mode;
    private final List<Slot> slots;
    private final List<Entry> entries = new ArrayList<Entry>();
    private final Input[] last;
    private int rulesVersion = RulesVersion.ID;

    public Replay(long seed, String arena, Mode mode, List<Slot> slots) {
        this.seed = seed;
        this.arena = arena;
        this.mode = mode;
        this.slots = Collections.unmodifiableList(new ArrayList<Slot>(slots));
        this.last = new Input[slots.size()];
    }

    public List<Slot> slots() {
        return slots;
    }

    public int rulesVersion() {
        return rulesVersion;
    }

    public Match newMatch() {
        if (rulesVersion != RulesVersion.ID) {
            throw new IllegalStateException("replay is rules " + rulesVersion + ", this build plays " + RulesVersion.ID);
        }
        return new Match(seed, Catalog.arena(arena), mode, slots);
    }

    /// Records what [Match#step] was given at `tick`; only changes are kept.
    public void record(int tick, Input[] inputs) {
        for (int i = 0; i < last.length && i < inputs.length; i++) {
            Input in = inputs[i];
            if (in == null || slots.get(i).bot() || in.equals(last[i])) {
                continue;
            }
            last[i] = in;
            entries.add(new Entry(tick, i, in));
        }
    }

    /// Runs a fresh match through `tick` ticks (or to the end) and answers it.
    public Match runTo(int tick) {
        Match m = newMatch();
        int e = 0;
        Input[] frame = new Input[slots.size()];
        while (m.tick() < tick && !m.over()) {
            for (int i = 0; i < frame.length; i++) {
                frame[i] = null;
            }
            while (e < entries.size() && entries.get(e).tick == m.tick()) {
                Entry en = entries.get(e++);
                frame[en.slot] = en.input;
            }
            m.step(frame);
        }
        return m;
    }

    public Match play() {
        return runTo(Integer.MAX_VALUE);
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(MAGIC).append(' ').append(rulesVersion).append('\n');
        sb.append(seed).append(' ').append(arena).append(' ').append(mode.name()).append('\n');
        sb.append(slots.size()).append('\n');
        for (int i = 0; i < slots.size(); i++) {
            sb.append(slots.get(i).encode()).append('\n');
        }
        for (int i = 0; i < entries.size(); i++) {
            Entry en = entries.get(i);
            sb.append(en.tick).append(' ').append(en.slot).append(' ').append(en.input.encode()).append('\n');
        }
        return sb.toString();
    }

    public static Replay decode(String text) {
        List<String> lines = Text.split(text, '\n');
        List<String> head = Text.words(lines.get(0));
        if (head.size() != 2 || !MAGIC.equals(head.get(0))) {
            throw new IllegalArgumentException("not a replay");
        }
        List<String> meta = Text.words(lines.get(1));
        int n = Integer.parseInt(lines.get(2).trim());
        List<Slot> slots = new ArrayList<Slot>();
        for (int i = 0; i < n; i++) {
            slots.add(Slot.decode(lines.get(3 + i)));
        }
        Replay r = new Replay(Long.parseLong(meta.get(0)), meta.get(1), Mode.byName(meta.get(2)), slots);
        r.rulesVersion = Integer.parseInt(head.get(1));
        for (int i = 3 + n; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.length() == 0) {
                continue;
            }
            int a = line.indexOf(' ');
            int b = line.indexOf(' ', a + 1);
            int tick = Integer.parseInt(line.substring(0, a));
            int slot = Integer.parseInt(line.substring(a + 1, b));
            r.entries.add(new Entry(tick, slot, Input.decode(line.substring(b + 1))));
        }
        return r;
    }

    private static final class Entry {
        final int tick;
        final int slot;
        final Input input;

        Entry(int tick, int slot, Input input) {
            this.tick = tick;
            this.slot = slot;
            this.input = input;
        }
    }
}
