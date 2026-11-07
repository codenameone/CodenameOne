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

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/// A progress bar with a percentage, a count, a rate and the time left:
///
/// ```text
/// upload  ##################--------------  58%  5.8/10.0 MB  1.2 MB/s  0:03
/// ```
///
/// (drawn with block characters on a UTF-8 terminal).
///
/// ```java
/// try (ProgressBar bar = new ProgressBar("upload", files.size())) {
///     for (File f : files) {
///         send(f);
///         bar.step();
///     }
/// }
/// ```
///
/// or, for a loop, [#track]:
///
/// ```java
/// for (File f : ProgressBar.track(files, "upload")) {
///     send(f);
/// }
/// ```
///
/// Stepping is cheap and safe from any thread. A bar made on its own draws
/// itself; several at once belong in a [MultiProgress]. Off a terminal the bar
/// prints milestone lines instead of redrawing (see [MultiProgress]).
public final class ProgressBar implements AutoCloseable {
    private static final String BLOCKS = " \u258f\u258e\u258d\u258c\u258b\u258a\u2589\u2588";

    private final MultiProgress owner;
    private final boolean ownsGroup;
    private final String label;
    private final long started;
    private final AtomicLong current = new AtomicLong();
    private final AtomicLong total;
    private final AtomicReference<String> message = new AtomicReference<String>();
    private final AtomicReference<String> unit = new AtomicReference<String>("");
    private final AtomicBoolean bytes = new AtomicBoolean();
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicBoolean failed = new AtomicBoolean();
    private final AtomicLong finishedAt = new AtomicLong();
    private final AtomicLong lastMilestone = new AtomicLong();
    private final AtomicLong lastMilestoneAt = new AtomicLong();

    /// A bar on its own, for `total` units of work; a total of 0 or less is
    /// unknown, and the bar then animates without a percentage.
    public ProgressBar(String label, long total) {
        this.owner = new MultiProgress();
        this.ownsGroup = true;
        this.label = label == null ? "" : label;
        this.total = new AtomicLong(total);
        this.started = Console.backend().currentTimeMillis();
        owner.adopt(this);
    }

    ProgressBar(MultiProgress group, String label, long total, long started) {
        this.owner = group;
        this.ownsGroup = false;
        this.label = label == null ? "" : label;
        this.total = new AtomicLong(total);
        this.started = started;
    }

    /// Wraps `items` so that iterating them steps a bar labelled `label`,
    /// which completes when the iteration ends. A [Collection]'s size is the
    /// total; any other iterable has an unknown one.
    public static <T> Iterable<T> track(final Iterable<T> items, final String label) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                long size = items instanceof Collection ? ((Collection<?>) items).size() : -1;
                final ProgressBar bar = new ProgressBar(label, size); // NOPMD CloseResource - closed when the iteration ends
                final Iterator<T> it = items.iterator();
                return new Iterator<T>() {
                    private boolean first = true;

                    @Override
                    public boolean hasNext() {
                        boolean more = it.hasNext();
                        if (!more) {
                            if (!first) {
                                bar.step();
                            }
                            first = true;
                            bar.close();
                        }
                        return more;
                    }

                    @Override
                    public T next() {
                        // An item counts once the loop body that took it is
                        // done, which is when the next one is asked for.
                        if (!first) {
                            bar.step();
                        }
                        first = false;
                        return it.next();
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }
        };
    }

    /// The label.
    public String label() {
        return label;
    }

    /// Units shown after the count, `files` in `3/10 files`.
    public ProgressBar unit(String unit) {
        this.unit.set(unit == null ? "" : unit);
        return this;
    }

    /// Shows the count, the total and the rate as sizes (`5.8 MB`) rather
    /// than numbers.
    public ProgressBar bytes() {
        this.bytes.set(true);
        return this;
    }

    /// Advances by one.
    public ProgressBar step() {
        return stepBy(1);
    }

    /// Advances by `n`.
    public ProgressBar stepBy(long n) {
        current.addAndGet(n);
        owner.changed(this);
        return this;
    }

    /// Sets the amount done.
    public ProgressBar stepTo(long n) {
        current.set(n);
        owner.changed(this);
        return this;
    }

    /// Changes the total; 0 or less makes it unknown.
    public ProgressBar setTotal(long total) {
        this.total.set(total);
        owner.changed(this);
        return this;
    }

    /// Shows `message` after the numbers: the file being worked on, say.
    public ProgressBar setMessage(String message) {
        this.message.set(message);
        owner.changed(this);
        return this;
    }

    /// The amount done.
    public long getCurrent() {
        return current.get();
    }

    /// The total, 0 or less when unknown.
    public long getTotal() {
        return total.get();
    }

    /// Whether the bar has completed or failed.
    public boolean isDone() {
        return done.get();
    }

    /// Marks the work complete: the bar fills and shows the time it took.
    public void complete() {
        long t = total.get();
        if (t > 0 && current.get() < t) {
            current.set(t);
        }
        finish(false);
    }

    /// Marks the work failed: the bar stops where it is, in red.
    public void fail() {
        finish(true);
    }

    private void finish(boolean failure) {
        if (done.compareAndSet(false, true)) {
            failed.set(failure);
            finishedAt.set(Console.backend().currentTimeMillis());
            owner.finished(this);
        }
    }

    /// Completes the bar if nothing else did and, for a bar made on its own,
    /// stops drawing it.
    @Override
    public void close() {
        if (!done.get()) {
            complete();
        }
        if (ownsGroup) {
            owner.close();
        }
    }

    /// One line of the bar at time `now`, `width` columns wide, with the
    /// label padded to `labelWidth`.
    String render(long now, int width, int labelWidth) {
        long cur = current.get();
        long tot = total.get();
        boolean isDone = done.get();
        boolean known = tot > 0;
        long end = isDone ? finishedAt.get() : now;
        long elapsed = Math.max(0, end - started);
        boolean unicode = Console.unicode();

        StringBuilder stats = new StringBuilder();
        if (known) {
            int pct = (int) Math.min(100.0, cur * 100.0 / tot);
            stats.append(TextWidth.pad(pct + "%", 4, Table.Align.RIGHT));
            stats.append("  ").append(amount(cur, false)).append('/').append(amount(tot, true));
        } else {
            stats.append(amount(cur, true));
        }
        if (elapsed > 0 && cur > 0) {
            double perSecond = cur * 1000.0 / elapsed;
            stats.append("  ").append(rate(perSecond));
        }
        if (isDone || !known) {
            stats.append("  ").append(duration(elapsed));
        } else if (cur > 0 && elapsed > 0) {
            long remaining = (long) (Math.max(0, tot - cur) * (double) elapsed / cur);
            stats.append("  ").append(duration(remaining));
        } else {
            stats.append("  -:--");
        }
        String msg = message.get();

        String prefix = TextWidth.pad(label, labelWidth, Table.Align.LEFT);
        int fixed = TextWidth.width(prefix) + 2 + 2 + stats.length();
        int barWidth = Math.max(10, Math.min(40, width - 1 - fixed));
        String bar;
        if (known || isDone) {
            double fraction = known ? Math.min(1.0, (double) cur / tot) : 1.0;
            bar = filled(fraction, barWidth, unicode);
        } else {
            bar = bounce(now - started, barWidth, unicode);
        }
        Console.Style color = failed.get() ? Console.Style.RED
                : isDone ? Console.Style.GREEN : Console.Style.CYAN;
        StringBuilder line = new StringBuilder();
        if (prefix.length() > 0) {
            line.append(prefix).append("  ");
        }
        line.append(Console.style(bar, color)).append("  ").append(stats);
        if (msg != null && msg.length() > 0) {
            line.append("  ").append(Console.style(msg, Console.Style.FAINT));
        }
        return line.toString();
    }

    static String filled(double fraction, int width, boolean unicode) {
        StringBuilder sb = new StringBuilder(width);
        if (!unicode) {
            int full = (int) (fraction * width);
            Table.repeat(sb, '#', full);
            Table.repeat(sb, '-', width - full);
            return sb.toString();
        }
        int eighths = (int) (fraction * width * 8);
        int full = eighths / 8;
        int part = eighths % 8;
        Table.repeat(sb, BLOCKS.charAt(8), full);
        if (full < width) {
            sb.append(part == 0 ? '\u2591' : BLOCKS.charAt(part));
            Table.repeat(sb, '\u2591', width - full - 1);
        }
        return sb.toString();
    }

    /// A three-cell block sliding back and forth, for an unknown total.
    static String bounce(long elapsed, int width, boolean unicode) {
        int block = 3;
        int span = Math.max(1, width - block);
        int step = (int) ((elapsed / MultiProgress.FRAME_MS) % (2 * span));
        int pos = step < span ? step : 2 * span - step;
        char on = unicode ? '\u2588' : '=';
        char off = unicode ? '\u2591' : ' ';
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            sb.append(i >= pos && i < pos + block ? on : off);
        }
        return sb.toString();
    }

    /// The line printed off a terminal when the bar has passed another tenth
    /// of its total, at most once a second; null when none is due.
    String milestone(long now) {
        long tot = total.get();
        if (tot <= 0 || done.get()) {
            return null;
        }
        long decile = (long) Math.min(10.0, current.get() * 10.0 / tot);
        long last = lastMilestone.get();
        if (decile <= last || decile >= 10 || now - lastMilestoneAt.get() < 1000) {
            return null;
        }
        if (!lastMilestone.compareAndSet(last, decile)) {
            return null;
        }
        lastMilestoneAt.set(now);
        return prefix() + (decile * 10) + "% (" + amount(current.get(), false) + '/'
                + amount(tot, true) + ')';
    }

    /// The final line printed off a terminal.
    String summary(long now) {
        long end = done.get() ? finishedAt.get() : now;
        String state = failed.get() ? "failed" : done.get() ? "done" : "stopped";
        long tot = total.get();
        String count = tot > 0 ? amount(current.get(), false) + '/' + amount(tot, true)
                : amount(current.get(), true);
        return prefix() + state + ' ' + count + " in " + duration(Math.max(0, end - started));
    }

    private String prefix() {
        return label.length() == 0 ? "" : label + ": ";
    }

    private String amount(long n, boolean withUnit) {
        if (bytes.get()) {
            return size(n);
        }
        String u = unit.get();
        return withUnit && u.length() > 0 ? n + " " + u : Long.toString(n);
    }

    private String rate(double perSecond) {
        if (bytes.get()) {
            return size((long) perSecond) + "/s";
        }
        String u = unit.get();
        String n = perSecond >= 10 ? Long.toString(Math.round(perSecond)) : oneDecimal(perSecond);
        return n + (u.length() > 0 ? " " + u : "") + "/s";
    }

    /// `n` bytes as a size with one decimal: `512 B`, `1.5 KB`, `3.2 GB`.
    static String size(long n) {
        if (n < 1024) {
            return n + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        double v = n;
        int i = -1;
        while (v >= 1024 && i < units.length - 1) {
            v /= 1024;
            i++;
        }
        return oneDecimal(v) + " " + units[i];
    }

    static String oneDecimal(double v) {
        long tenths = Math.round(v * 10);
        return (tenths / 10) + "." + (tenths % 10);
    }

    /// `ms` as `m:ss`, or `h:mm:ss` from an hour.
    static String duration(long ms) {
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s / 60) % 60;
        long sec = s % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append(':');
            if (m < 10) {
                sb.append('0');
            }
        }
        sb.append(m).append(':');
        if (sec < 10) {
            sb.append('0');
        }
        return sb.append(sec).toString();
    }
}
