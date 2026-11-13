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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/// An animated indicator for work of unknown length, ending in a status line:
///
/// ```java
/// try (Spinner s = Spinner.start("Resolving dependencies")) {
///     resolve();
///     s.succeed("Resolved 42 dependencies");
/// }
/// ```
///
/// A spinner closed without a status clears its line. Off a terminal it
/// prints the message once when it starts and the status when it ends.
public final class Spinner implements AutoCloseable {
    /// The animation frames.
    public enum Frames {
        /// Braille dots, the default on a UTF-8 terminal.
        DOTS("\u280b\u2819\u2839\u2838\u283c\u2834\u2826\u2827\u2807\u280f", 80),
        /// A quarter circle going round.
        ARC("\u25dc\u25e0\u25dd\u25de\u25e1\u25df", 100),
        /// A dot bouncing between two bars.
        BOUNCE("\u2801\u2802\u2804\u2802", 120),
        /// A line turning, for any terminal; the default otherwise.
        LINE("-\\|/", 130);

        private final String frames;
        private final int interval;

        Frames(String frames, int interval) {
            this.frames = frames;
            this.interval = interval;
        }

        String frame(long tick) {
            int cp = (int) (tick % frames.length());
            return frames.substring(cp, cp + 1);
        }
    }

    private final TerminalBackend term;
    private final LiveRegion region;
    private final AtomicReference<String> message;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<Thread> thread = new AtomicReference<Thread>();
    private final long started;
    private Frames frames;

    /// A spinner showing `message`, not yet started.
    public Spinner(String message) {
        this(Console.backend(), Console.isTerminal(), message);
    }

    Spinner(TerminalBackend term, boolean live, String message) {
        this.term = term;
        this.region = new LiveRegion(term, live);
        this.message = new AtomicReference<String>(message == null ? "" : message);
        this.started = term.currentTimeMillis();
        this.frames = Console.unicode() ? Frames.DOTS : Frames.LINE;
    }

    /// Makes a spinner showing `message` and starts it.
    public static Spinner start(String message) {
        return new Spinner(message).start();
    }

    /// Chooses the animation; call before [#start].
    public Spinner frames(Frames frames) {
        this.frames = frames;
        return this;
    }

    /// Starts animating.
    public Spinner start() {
        if (!running.compareAndSet(false, true)) {
            return this;
        }
        if (!region.isLive()) {
            term.write(message.get() + "...\n");
            return this;
        }
        draw();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running.get()) {
                    try {
                        Thread.sleep(frames.interval);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (running.get()) {
                        draw();
                    }
                }
            }
        }, "cn1-spinner");
        t.setDaemon(true);
        thread.set(t);
        t.start();
        return this;
    }

    /// Changes the message while spinning.
    public void setMessage(String text) {
        message.set(text == null ? "" : text);
        if (running.get()) {
            draw();
        }
    }

    /// Prints a line above the spinner.
    public void println(String text) {
        region.println(text);
    }

    void draw() {
        long tick = (term.currentTimeMillis() - started) / frames.interval;
        region.update(Console.style(frames.frame(tick), Console.Style.CYAN) + " " + message.get());
    }

    /// Stops with a green check and `text` (the message when null).
    public void succeed(String text) {
        stop(Console.unicode() ? "\u2714" : "v", Console.Style.GREEN, text);
    }

    /// Stops with a red cross and `text`.
    public void fail(String text) {
        stop(Console.unicode() ? "\u2716" : "x", Console.Style.RED, text);
    }

    /// Stops with a yellow warning sign and `text`.
    public void warn(String text) {
        stop(Console.unicode() ? "\u26a0" : "!", Console.Style.YELLOW, text);
    }

    /// Stops with a blue information sign and `text`.
    public void info(String text) {
        stop(Console.unicode() ? "\u2139" : "i", Console.Style.BLUE, text);
    }

    private void stop(String symbol, Console.Style color, String text) {
        if (!halt()) {
            return;
        }
        String line = Console.style(symbol, color) + " " + (text == null ? message.get() : text);
        if (region.isLive()) {
            region.updateNow(line);
            region.close();
        } else {
            term.write(line + "\n");
        }
    }

    /// Stops and clears the line, unless a status was already shown.
    @Override
    public void close() {
        if (halt()) {
            region.clear();
            region.close();
        }
    }

    private boolean halt() {
        if (!running.getAndSet(false)) {
            return false;
        }
        Thread t = thread.getAndSet(null);
        if (t != null) {
            t.interrupt();
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return true;
    }
}
