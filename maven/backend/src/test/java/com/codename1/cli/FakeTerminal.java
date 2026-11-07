/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.cli;

import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/// A scripted terminal: input bytes queued in advance, output captured, the
/// environment and clock set by the test.
final class FakeTerminal implements TerminalBackend {
    final Map<String, String> env = new HashMap<String, String>();
    final StringBuilder out = new StringBuilder();
    final StringBuilder err = new StringBuilder();
    private final Deque<Integer> input = new ArrayDeque<Integer>();
    boolean stdinTty;
    boolean stdoutTty;
    boolean rawAllowed = true;
    boolean raw;
    int rawEntries;
    int columns = 80;
    int rows = 24;
    long now = 1000;

    /// A terminal with standard input and output both terminals when `tty`.
    static FakeTerminal tty(boolean tty) {
        FakeTerminal t = new FakeTerminal();
        t.stdinTty = tty;
        t.stdoutTty = tty;
        t.env.put("TERM", "xterm-256color");
        t.env.put("NO_COLOR", "1");
        t.env.put("LANG", "en_US.UTF-8");
        return t;
    }

    FakeTerminal type(String text) {
        try {
            for (byte b : text.getBytes("UTF-8")) {
                input.add(b & 0xff);
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        return this;
    }

    FakeTerminal bytes(int... values) {
        for (int v : values) {
            input.add(v);
        }
        return this;
    }

    FakeTerminal up() {
        return type("\u001b[A");
    }

    FakeTerminal down() {
        return type("\u001b[B");
    }

    FakeTerminal enter() {
        return type("\r");
    }

    String output() {
        return out.toString();
    }

    @Override
    public boolean isTerminal(int fd) {
        return fd == 0 ? stdinTty : stdoutTty;
    }

    @Override
    public boolean isWindows() {
        return false;
    }

    @Override
    public int size() {
        return (columns << 16) | rows;
    }

    @Override
    public boolean enterRaw() {
        if (!rawAllowed || !stdinTty) {
            return false;
        }
        raw = true;
        rawEntries++;
        return true;
    }

    @Override
    public void restore() {
        raw = false;
    }

    @Override
    public int readByte(int timeoutMs) {
        if (input.isEmpty()) {
            return timeoutMs >= 0 ? -2 : -1;
        }
        return input.removeFirst();
    }

    @Override
    public void write(String text) {
        out.append(text);
    }

    @Override
    public void writeError(String text) {
        err.append(text);
    }

    @Override
    public String getenv(String name) {
        return env.get(name);
    }

    @Override
    public long currentTimeMillis() {
        return now;
    }

    String hidden;

    @Override
    public String readHidden() {
        return hidden;
    }

    @Override
    public void cursorHidden(boolean hidden) {
        this.cursorHiddenFlag = hidden;
    }

    boolean cursorHiddenFlag;
}
