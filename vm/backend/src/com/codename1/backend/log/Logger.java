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

/// An application logger in the shape slf4j's is, writing through [Logging].
///
/// ```java
/// private static final Logger LOG = Logger.getLogger(PushService.class);
///
/// LOG.info("sent {} notifications to {}", count, appId);
/// LOG.warn("APNs rejected token", ex);
/// if (LOG.isDebugEnabled()) {
///     LOG.debug("payload {}", render(payload));
/// }
/// ```
///
/// Each `{}` in the message is replaced by the next argument; `\{}` is a
/// literal `{}`. A throwable left over after the placeholders are filled -- the
/// last argument, as slf4j takes it -- is written with its stack trace.
///
/// A line reads `2026-11-01T09:30:00.123Z INFO  [thread] com.example.Push - message`,
/// followed by the thread's [MDC] context when it has one. The level of each
/// logger comes from the `logging.level.*` settings; see [Logging].
///
/// No reflection, no configuration file and no static binding: a logger is a
/// name and a cached level.
public final class Logger {
    private final String name;
    /// The level last resolved and the configuration generation it came from.
    /// Read and written without synchronisation on purpose: a racing reader at
    /// worst resolves the level again, which gives the same answer.
    private Level level;
    private int generation = -1;

    private Logger(String name) {
        this.name = name;
    }

    /// The logger named after `type`, its fully qualified class name.
    public static Logger getLogger(Class<?> type) {
        return new Logger(type.getName());
    }

    /// The logger with `name`.
    public static Logger getLogger(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        return new Logger(name);
    }

    /// The name lines are written under.
    public String getName() {
        return name;
    }

    /// Whether a line at `level` would be written.
    public boolean isEnabled(Level level) {
        return level != Level.OFF && level.compareTo(effectiveLevel()) >= 0;
    }

    public boolean isTraceEnabled() {
        return isEnabled(Level.TRACE);
    }

    public boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG);
    }

    public boolean isInfoEnabled() {
        return isEnabled(Level.INFO);
    }

    public boolean isWarnEnabled() {
        return isEnabled(Level.WARN);
    }

    public boolean isErrorEnabled() {
        return isEnabled(Level.ERROR);
    }

    public void trace(String message, Object... args) {
        log(Level.TRACE, message, args);
    }

    public void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }

    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public void warn(String message, Object... args) {
        log(Level.WARN, message, args);
    }

    public void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }

    /// Writes `message` at `level`, its `{}` placeholders filled from `args`.
    public void log(Level level, String message, Object... args) {
        if (!isEnabled(level)) {
            return;
        }
        StringBuilder line = new StringBuilder(64 + (message == null ? 4 : message.length()));
        Timestamps.appendIso(line, System.currentTimeMillis());
        String label = level.name();
        line.append(' ').append(label);
        for (int i = label.length(); i < 5; i++) {
            line.append(' ');
        }
        line.append(" [").append(Thread.currentThread().getName()).append("] ").append(name).append(" - ");
        int used = format(line, message, args);
        Throwable error = null;
        if (args != null && used < args.length && args[args.length - 1] instanceof Throwable) {
            error = (Throwable) args[args.length - 1];
        }
        MDC.appendTo(line);
        Logging.write(level, name, line.toString(), error);
    }

    private Level effectiveLevel() {
        int current = Logging.generation();
        Level cached = level;
        if (cached == null || generation != current) {
            cached = Logging.levelOf(name);
            level = cached;
            generation = current;
        }
        return cached;
    }

    /// Appends `message` with each `{}` replaced by the next argument, and
    /// answers how many arguments it used.
    static int format(StringBuilder out, String message, Object[] args) {
        if (message == null) {
            out.append("null");
            return 0;
        }
        int used = 0;
        int count = args == null ? 0 : args.length;
        int length = message.length();
        for (int i = 0; i < length; i++) {
            char c = message.charAt(i);
            if (c == '\\' && i + 2 < length && message.charAt(i + 1) == '{' && message.charAt(i + 2) == '}') {
                out.append("{}");
                i += 2;
            } else if (c == '{' && i + 1 < length && message.charAt(i + 1) == '}' && used < count) {
                appendArgument(out, args[used++]);
                i++;
            } else {
                out.append(c);
            }
        }
        return used;
    }

    private static void appendArgument(StringBuilder out, Object value) {
        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            out.append('[');
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    out.append(", ");
                }
                out.append(array[i]);
            }
            out.append(']');
        } else {
            out.append(value);
        }
    }
}
