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
package com.codename1.backend.log;

import com.codename1.backend.Config;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// The logging configuration: per-logger levels and where lines go.
///
/// Levels come from `logging.level.<name>` settings, the keys Spring Boot uses.
/// A logger takes the level of the longest name that is it or a package above
/// it, and `logging.level.root` (INFO when unset) covers everything else:
///
/// ```properties
/// logging.level.root=WARN
/// logging.level.com.example=INFO
/// logging.level.com.example.push=DEBUG
/// ```
///
/// The server applies its configuration when it starts; [#setLevel(String, Level)]
/// changes one at run time, and every logger sees the change on its next line.
public final class Logging {
    /// The prefix of the per-logger level settings.
    public static final String LEVEL_PREFIX = "logging.level.";

    /// Name/level pairs, longest name first, then the root level last.
    private static final AtomicReference<Object[]> RULES = new AtomicReference<Object[]>(
            new Object[] {"", Level.INFO});
    /// Bumped on every change, so a logger knows its cached level is stale.
    private static final AtomicInteger GENERATION = new AtomicInteger();
    private static final AtomicReference<LogSink> SINK = new AtomicReference<LogSink>(new StandardSink(System.out));

    private Logging() {
    }

    /// Reads every `logging.level.*` setting of `config`, replacing the levels
    /// set before. Called by the server when it starts.
    ///
    /// @throws IOException when a setting names no level
    public static void configure(Config config) throws IOException {
        List<Object> rules = new ArrayList<Object>();
        Level root = Level.INFO;
        for (Object key : config.keys()) {
            String name = String.valueOf(key);
            if (!name.startsWith(LEVEL_PREFIX)) {
                continue;
            }
            String logger = name.substring(LEVEL_PREFIX.length());
            String value = config.get(name, null);
            Level level = Level.parse(value);
            if (level == null) {
                throw new IOException(name + "=" + value + " names no level; use TRACE, DEBUG, INFO, WARN, ERROR or OFF");
            }
            if ("root".equals(logger)) {
                root = level;
            } else {
                rules.add(logger);
                rules.add(level);
            }
        }
        install(rules, root);
    }

    /// Sets the level of `logger` and every logger under it; null clears a level
    /// set before. `"root"` is the level of everything else.
    public static void setLevel(String logger, Level level) {
        while (true) {
            Object[] current = RULES.get();
            List<Object> rules = new ArrayList<Object>();
            Level root = (Level) current[current.length - 1];
            for (int i = 0; i + 2 < current.length; i += 2) {
                if (!current[i].equals(logger)) {
                    rules.add(current[i]);
                    rules.add(current[i + 1]);
                }
            }
            if ("root".equals(logger)) {
                root = level == null ? Level.INFO : level;
            } else if (level != null) {
                rules.add(logger);
                rules.add(level);
            }
            if (RULES.compareAndSet(current, sorted(rules, root))) {
                GENERATION.incrementAndGet();
                return;
            }
        }
    }

    /// Sends every line to `sink` from now on; null restores standard output.
    public static void setSink(LogSink sink) {
        SINK.set(sink == null ? new StandardSink(System.out) : sink);
    }

    private static void install(List<Object> rules, Level root) {
        RULES.set(sorted(rules, root));
        GENERATION.incrementAndGet();
    }

    /// The pairs, longest name first, so the first match is the most specific,
    /// with the root level as the last element.
    private static Object[] sorted(List<Object> rules, Level root) {
        int pairs = rules.size() / 2;
        String[] names = new String[pairs];
        Level[] levels = new Level[pairs];
        for (int i = 0; i < pairs; i++) {
            names[i] = (String) rules.get(i * 2);
            levels[i] = (Level) rules.get(i * 2 + 1);
        }
        for (int i = 1; i < pairs; i++) {
            for (int j = i; j > 0 && names[j].length() > names[j - 1].length(); j--) {
                String n = names[j];
                names[j] = names[j - 1];
                names[j - 1] = n;
                Level l = levels[j];
                levels[j] = levels[j - 1];
                levels[j - 1] = l;
            }
        }
        Object[] out = new Object[pairs * 2 + 1];
        for (int i = 0; i < pairs; i++) {
            out[i * 2] = names[i];
            out[i * 2 + 1] = levels[i];
        }
        out[pairs * 2] = root;
        return out;
    }

    static int generation() {
        return GENERATION.get();
    }

    /// The level `logger` is set to: that of the longest configured name equal
    /// to it or a dotted prefix of it, else the root level.
    static Level levelOf(String logger) {
        Object[] rules = RULES.get();
        for (int i = 0; i + 2 < rules.length; i += 2) {
            String name = (String) rules[i];
            if (logger.startsWith(name) && (logger.length() == name.length() || logger.charAt(name.length()) == '.'
                    || logger.charAt(name.length()) == '$')) {
                return (Level) rules[i + 1];
            }
        }
        return (Level) rules[rules.length - 1];
    }

    static void write(Level level, String logger, String line, Throwable error) {
        SINK.get().write(level, logger, line, error);
    }

    /// Writes each line, and the stack trace after it, to one stream.
    private static final class StandardSink implements LogSink {
        private final PrintStream out;

        StandardSink(PrintStream out) {
            this.out = out;
        }

        @Override
        public void write(Level level, String logger, String line, Throwable error) {
            // One println, so lines from two threads never interleave mid-line.
            out.println(line);
            if (error != null) {
                error.printStackTrace(out);
            }
        }
    }
}
