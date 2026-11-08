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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/// Several progress bars drawn together, one line each, for work that runs in
/// parallel:
///
/// ```java
/// try (MultiProgress all = new MultiProgress()) {
///     ProgressBar a = all.add("images", 120);
///     ProgressBar b = all.add("videos", 8);
///     // ... a.step() and b.step() from any thread ...
///     all.println("skipped broken.png");
/// }
/// ```
///
/// On a terminal the bars are redrawn ten times a second by a background
/// thread, so a bar that is not stepped still shows its rate and an unknown
/// total still animates; stepping only updates numbers and is cheap from any
/// thread. Off a terminal each bar prints a line as it passes every tenth of
/// its total (at most one a second) and a final line when it completes.
public final class MultiProgress implements AutoCloseable {
    static final int FRAME_MS = 100;

    private final TerminalBackend term;
    private final LiveRegion region;
    private final boolean animate;
    private final AtomicReference<List<ProgressBar>> bars =
            new AtomicReference<List<ProgressBar>>(new ArrayList<ProgressBar>());
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<Thread> ticker = new AtomicReference<Thread>();

    /// A group on standard output.
    public MultiProgress() {
        this(Console.backend(), Console.isTerminal(), true);
    }

    MultiProgress(TerminalBackend term, boolean live, boolean animate) {
        this.term = term;
        this.region = new LiveRegion(term, live);
        this.animate = animate;
    }

    /// Adds a bar for `total` units of work; a total of 0 or less is unknown
    /// and the bar animates without a percentage.
    public ProgressBar add(String label, long total) {
        ProgressBar bar = new ProgressBar(this, label, total, term.currentTimeMillis());
        adopt(bar);
        return bar;
    }

    /// Adds a bar made elsewhere: a [ProgressBar] made on its own adopts
    /// itself into a group of one.
    void adopt(ProgressBar bar) {
        List<ProgressBar> current;
        List<ProgressBar> next;
        do {
            current = bars.get();
            next = new ArrayList<ProgressBar>(current);
            next.add(bar);
        } while (!bars.compareAndSet(current, next));
        startTicker();
        redraw();
    }

    /// Prints `text` above the bars.
    public void println(String text) {
        region.println(text);
    }

    /// Whether the bars redraw in place.
    public boolean isLive() {
        return region.isLive();
    }

    /// Draws every bar now. The ticker's frames may be dropped while another
    /// is going out; a frame the caller asked for -- a change with no
    /// ticker, a bar finishing -- carries state no later frame may repeat,
    /// so it waits its turn instead.
    void redraw() {
        redraw(false);
    }

    private void redraw(boolean mustDraw) {
        if (!region.isLive() || closed.get()) {
            return;
        }
        String[] f = frame(term.currentTimeMillis());
        if (mustDraw) {
            region.updateNow(f);
        } else {
            region.update(f);
        }
    }

    String[] frame(long now) {
        List<ProgressBar> list = bars.get();
        // Measured across the group first, so every bar is drawn at one
        // width and the bars and their numbers line up in columns.
        int labelWidth = 0;
        int statsWidth = 0;
        int messageWidth = 0;
        for (ProgressBar bar : list) { // NOPMD CloseResource - the group owns its bars
            labelWidth = Math.max(labelWidth, TextWidth.width(bar.label()));
            statsWidth = Math.max(statsWidth, bar.stats(now).length());
            messageWidth = Math.max(messageWidth, bar.messageWidth());
        }
        int width = Console.width();
        String[] lines = new String[list.size()];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = list.get(i).render(now, width, labelWidth, statsWidth, messageWidth);
        }
        return lines;
    }

    /// Called by a bar after it changed; off a terminal this is where the
    /// milestone lines come from.
    void changed(ProgressBar bar) {
        if (region.isLive()) {
            if (!animate) {
                redraw(true);
            }
            return;
        }
        String line = bar.milestone(term.currentTimeMillis());
        if (line != null) {
            region.println(line);
        }
    }

    /// Called by a bar that has finished, so its final line is printed now.
    void finished(ProgressBar bar) {
        if (region.isLive()) {
            redraw(true);
        } else {
            region.println(bar.summary(term.currentTimeMillis()));
        }
    }

    private void startTicker() {
        if (!animate || !region.isLive() || !running.compareAndSet(false, true)) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running.get()) {
                    redraw();
                    try {
                        Thread.sleep(FRAME_MS);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }, "cn1-progress");
        t.setDaemon(true);
        ticker.set(t);
        t.start();
    }

    /// Stops redrawing and leaves the final state of every bar on screen;
    /// bars still running are shown as they are.
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        running.set(false);
        Thread t = ticker.getAndSet(null);
        if (t != null) {
            t.interrupt();
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (region.isLive()) {
            region.updateNow(frame(term.currentTimeMillis()));
        } else {
            for (ProgressBar bar : bars.get()) { // NOPMD CloseResource - closing the group
                if (!bar.isDone()) {
                    region.println(bar.summary(term.currentTimeMillis()));
                }
            }
        }
        region.close();
    }
}
