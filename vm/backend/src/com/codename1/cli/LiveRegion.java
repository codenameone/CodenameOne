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

/// The last few lines of the terminal, redrawn in place: what progress bars,
/// spinners and prompts are drawn on.
///
/// [#update] replaces the lines; [#println] prints a line above them, so log
/// output and a live display can be mixed without tearing. Each frame goes out
/// in one write. Lines are cut to the terminal's width, because a line that
/// wrapped would throw off the count of lines to move back over.
///
/// When standard output is not a terminal nothing is redrawn: [#println]
/// prints its line and [#close] prints the final lines once, so a log gets the
/// outcome without a frame per update.
public final class LiveRegion implements AutoCloseable {
    private final TerminalBackend term;
    private final boolean live;
    private final AtomicBoolean busy = new AtomicBoolean();
    private String[] lines = new String[0];
    /// Lines currently on screen from the last frame.
    private int drawn;
    /// The row of the region the cursor was left on after the last frame.
    private int cursorRow;
    private boolean cursorHidden;
    private boolean closed;

    /// A region on standard output, live when it is a terminal.
    public LiveRegion() {
        this(Console.backend(), Console.isTerminal());
    }

    LiveRegion(TerminalBackend term, boolean live) {
        this.term = term;
        this.live = live;
    }

    /// Whether this region redraws in place.
    public boolean isLive() {
        return live;
    }

    /// Replaces the lines and redraws them, with the cursor hidden.
    public void update(String... newLines) {
        update(newLines, -1, 0, false);
    }

    /// Replaces the lines, waiting for a frame already going out rather than
    /// being dropped.
    void updateNow(String... newLines) {
        update(newLines, -1, 0, true);
    }

    /// Replaces the lines and leaves the cursor visible at `row`, `column`
    /// (both from 0) of the region, the way a prompt shows where typing goes.
    void updateWithCursor(String[] newLines, int row, int column) {
        update(newLines, row, column, true);
    }

    private void update(String[] newLines, int row, int column, boolean mustDraw) {
        if (closed) {
            return;
        }
        if (!acquire(mustDraw)) {
            // A frame from an animation thread while another frame is going
            // out: dropping it is harmless, the next one carries the state.
            return;
        }
        try {
            // Checked again under the flag: close() may have finished while
            // this frame waited, and a frame drawn after it would land below
            // the final state and hide the cursor again.
            if (closed) {
                return;
            }
            lines = fit(newLines);
            if (live) {
                StringBuilder sb = new StringBuilder();
                moveToTop(sb);
                drawLines(sb);
                placeCursor(sb, Math.min(row, lines.length - 1), column);
                term.write(sb.toString());
            }
        } finally {
            busy.set(false);
        }
    }

    /// Prints `text` above the region and redraws the region under it.
    public void println(String text) {
        acquire(true);
        try {
            if (!live) {
                term.write(text + "\n");
                return;
            }
            StringBuilder sb = new StringBuilder();
            moveToTop(sb);
            sb.append(Ansi.eraseDown()).append(text).append('\n');
            drawn = 0;
            cursorRow = 0;
            if (!closed) {
                drawLines(sb);
                placeCursor(sb, -1, 0);
            }
            term.write(sb.toString());
        } finally {
            busy.set(false);
        }
    }

    /// Erases the region from the screen, leaving the cursor where it began.
    public void clear() {
        acquire(true);
        try {
            if (live && drawn > 0) {
                StringBuilder sb = new StringBuilder();
                moveToTop(sb);
                sb.append(Ansi.eraseDown());
                if (cursorHidden) {
                    sb.append(Ansi.showCursor());
                    cursorHidden = false;
                    term.cursorHidden(false);
                }
                term.write(sb.toString());
            }
            drawn = 0;
            cursorRow = 0;
            lines = new String[0];
        } finally {
            busy.set(false);
        }
    }

    /// Leaves the last lines on screen, moves below them and shows the
    /// cursor. Off a terminal, prints the last lines.
    @Override
    public void close() {
        acquire(true);
        try {
            if (closed) {
                return;
            }
            closed = true;
            StringBuilder sb = new StringBuilder();
            if (live) {
                if (drawn > 0) {
                    sb.append(Ansi.cursorDown(drawn - 1 - cursorRow)).append('\n');
                }
                if (cursorHidden) {
                    sb.append(Ansi.showCursor());
                    cursorHidden = false;
                    term.cursorHidden(false);
                }
            } else {
                for (String line : lines) {
                    sb.append(line).append('\n');
                }
            }
            if (sb.length() > 0) {
                term.write(sb.toString());
            }
        } finally {
            busy.set(false);
        }
    }

    /// One entry per screen line: an embedded line break would put more rows
    /// on screen than the region counts, and every redraw would then leave
    /// lines behind. A frame taller than the terminal cannot be redrawn in
    /// place either -- the cursor cannot move above the top of the screen --
    /// so it is clipped to the rows there are.
    private String[] fit(String[] newLines) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (newLines != null) {
            for (String l : newLines) {
                // A tab's width depends on where it lands; four spaces keep
                // the count of columns true.
                String line = l == null ? "" : l.replace("\t", "    ");
                int start = 0;
                for (int i = line.indexOf('\n'); i >= 0; i = line.indexOf('\n', start)) {
                    out.add(line.substring(start, i));
                    start = i + 1;
                }
                out.add(line.substring(start));
            }
        }
        int max = Math.max(1, Console.height(term) - 1);
        while (out.size() > max) {
            out.remove(out.size() - 1);
        }
        return out.toArray(new String[0]);
    }

    private boolean acquire(boolean wait) {
        while (!busy.compareAndSet(false, true)) {
            if (!wait) {
                return false;
            }
            Thread.yield();
        }
        return true;
    }

    private void moveToTop(StringBuilder sb) {
        sb.append('\r');
        if (drawn > 0) {
            sb.append(Ansi.cursorUp(cursorRow));
        }
    }

    private void drawLines(StringBuilder sb) {
        int width = Console.width();
        int n = lines.length;
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(Ansi.eraseLine()).append(TextWidth.truncate(lines[i], width - 1, ""));
        }
        if (n == 0) {
            sb.append(Ansi.eraseDown());
        } else if (n < drawn) {
            // The old frame was taller: wipe what is left of it below.
            sb.append('\n').append(Ansi.eraseDown()).append(Ansi.cursorUp(1))
                    .append(Ansi.cursorColumn(TextWidth.width(
                            TextWidth.truncate(lines[n - 1], width - 1, "")) + 1));
        }
        drawn = n;
        cursorRow = n == 0 ? 0 : n - 1;
    }

    private void placeCursor(StringBuilder sb, int row, int column) {
        if (row < 0 || drawn == 0) {
            if (!cursorHidden && drawn > 0) {
                sb.append(Ansi.hideCursor());
                cursorHidden = true;
                term.cursorHidden(true);
            }
            return;
        }
        int target = Math.min(row, drawn - 1);
        sb.append(Ansi.cursorUp(cursorRow - target)).append(Ansi.cursorColumn(column + 1));
        cursorRow = target;
        if (cursorHidden) {
            sb.append(Ansi.showCursor());
            cursorHidden = false;
            term.cursorHidden(false);
        }
    }
}
