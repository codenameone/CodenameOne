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
package com.codename1.backend;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The console lines a development server printed, for the MCP log tool.
 *
 * <p>The Java SE arm tees {@code System.out} and {@code System.err} into a ring of
 * recent lines; the packaged runtime has no way to replace them and keeps
 * nothing, which is why the development tools say so when asked there.
 */
public final class DevConsole {
    private static final int CAPACITY = 2000;
    private static final String[] LINES = new String[CAPACITY];
    private static int next;
    private static int size;
    private static boolean installed;

    private DevConsole() {
    }

    /** Whether this runtime can capture the console at all. */
    public static boolean supported() {
        return true;
    }

    /** Starts capturing. Idempotent. */
    public static synchronized void install() {
        if(installed) {
            return;
        }
        installed = true;
        System.setOut(new PrintStream(new Tee(System.out, "out"), true));
        System.setErr(new PrintStream(new Tee(System.err, "err"), true));
    }

    /** The newest {@code limit} lines, oldest first. */
    public static synchronized List recent(int limit) {
        int count = Math.min(limit, size);
        List out = new ArrayList(count);
        for(int iter = count - 1 ; iter >= 0 ; iter--) {
            out.add(LINES[(next - 1 - iter + CAPACITY) % CAPACITY]);
        }
        return out;
    }

    static synchronized void add(String line) {
        LINES[next] = line;
        next = (next + 1) % CAPACITY;
        if(size < CAPACITY) {
            size++;
        }
    }

    /** Writes through to the real stream and collects whole lines. */
    private static final class Tee extends OutputStream {
        private final PrintStream target;
        private final String name;
        private final java.io.ByteArrayOutputStream line = new java.io.ByteArrayOutputStream();

        Tee(PrintStream target, String name) {
            this.target = target;
            this.name = name;
        }

        public synchronized void write(int b) {
            target.write(b);
            if(b == '\n') {
                add("[" + name + "] " + new String(line.toByteArray(),
                        java.nio.charset.StandardCharsets.UTF_8));
                line.reset();
            } else if(b != '\r' && line.size() < 4000) {
                line.write(b);
            }
        }

        public void flush() {
            target.flush();
        }
    }
}
