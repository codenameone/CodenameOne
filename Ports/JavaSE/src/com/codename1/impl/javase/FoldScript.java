/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/// A timed sequence of fold changes, played against a foldable skin while a
/// recording runs, so one call produces a repeatable promo clip:
///
/// ```json
/// [{"at": 1500, "posture": "half"},
///  {"at": 4000, "posture": "closed"},
///  {"at": 7000, "angle": 180, "animateMs": 1200},
///  {"at": 9000, "orientation": "landscape"}]
/// ```
///
/// `at` is milliseconds from the start; each step names a `posture` (flat,
/// half, closed) or an `angle`, and optionally `animateMs`, or turns the
/// device to an `orientation` (portrait, landscape).
final class FoldScript {

    interface Target {
        void fold(String posture, int angle, long animateMs);

        void rotate(boolean portrait);
    }

    static final class Step {
        final long at;
        final String posture;
        final int angle;
        final long animateMs;
        /// "portrait" or "landscape" for a rotation step, otherwise null.
        final String orientation;

        Step(long at, String posture, int angle, long animateMs) {
            this(at, posture, angle, animateMs, null);
        }

        Step(long at, String posture, int angle, long animateMs, String orientation) {
            this.at = at;
            this.orientation = orientation;
            this.posture = posture;
            this.angle = angle;
            this.animateMs = animateMs;
        }
    }

    private final List<Step> steps;
    private Thread thread;
    private boolean cancelled;

    private FoldScript(List<Step> steps) {
        this.steps = steps;
    }

    /// Parses the JSON list (as `JSONParser` decodes it). Throws
    /// IllegalArgumentException naming the first bad step.
    static FoldScript parse(Object json) {
        if (!(json instanceof List)) {
            throw new IllegalArgumentException("script must be a list of steps");
        }
        List<Step> out = new ArrayList<Step>();
        int index = 0;
        for (Object o : (List<?>) json) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("script step " + index + " is not an object");
            }
            Map<?, ?> m = (Map<?, ?>) o;
            long at = number(m.get("at"), -1);
            if (at < 0) {
                throw new IllegalArgumentException("script step " + index + " needs a non-negative 'at'");
            }
            Object p = m.get("posture");
            String posture = p instanceof String ? (String) p : null;
            int angle = (int) number(m.get("angle"), -1);
            Object or = m.get("orientation");
            String orientation = or instanceof String ? (String) or : null;
            if (orientation != null && !"portrait".equals(orientation) && !"landscape".equals(orientation)) {
                throw new IllegalArgumentException("script step " + index + ": unknown orientation " + orientation);
            }
            if (posture == null && angle < 0 && orientation == null) {
                throw new IllegalArgumentException("script step " + index
                        + " needs a 'posture', an 'angle' or an 'orientation'");
            }
            if (posture != null && !FoldableSkin.POSTURE_FLAT.equals(posture)
                    && !FoldableSkin.POSTURE_HALF.equals(posture)
                    && !FoldableSkin.POSTURE_CLOSED.equals(posture)) {
                throw new IllegalArgumentException("script step " + index + ": unknown posture " + posture);
            }
            out.add(new Step(at, posture, angle, number(m.get("animateMs"), -1), orientation));
            index++;
        }
        Collections.sort(out, new Comparator<Step>() {
            public int compare(Step a, Step b) {
                return a.at < b.at ? -1 : (a.at > b.at ? 1 : 0);
            }
        });
        return new FoldScript(out);
    }

    private static long number(Object o, long def) {
        return o instanceof Number ? ((Number) o).longValue() : def;
    }

    /// True when a step folds rather than only rotates.
    boolean foldsTheDevice() {
        for (Step s : steps) {
            if (s.orientation == null) {
                return true;
            }
        }
        return false;
    }

    List<Step> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /// Plays the steps on a daemon thread, timed from now.
    synchronized void start(final Target target) {
        startWithTail(target, null, 0);
    }

    /// As `start`, then runs `done` (on the script's thread) once the last
    /// step has played and `tailMs` more have passed, unless cancelled.
    synchronized void startWithTail(final Target target, final Runnable done, final long tailMs) {
        final long start = AnimationClock.now();
        thread = new Thread(new Runnable() {
            public void run() {
                for (Step s : steps) {
                    // Polled rather than slept in one go: during a lockstep
                    // recording the clock follows the video, not the wall.
                    if (!waitUntil(start + s.at)) {
                        return;
                    }
                    if (isCancelled()) {
                        return;
                    }
                    if (s.orientation != null) {
                        target.rotate("portrait".equals(s.orientation));
                    } else {
                        target.fold(s.posture, s.angle, s.animateMs);
                    }
                }
                if (done != null) {
                    if (!waitUntil(AnimationClock.now() + Math.max(0, tailMs))) {
                        return;
                    }
                    if (!isCancelled()) {
                        done.run();
                    }
                }
            }
        }, "FoldScript");
        thread.setDaemon(true);
        thread.start();
    }

    /// Waits until the animation clock reaches `at`; false if interrupted.
    private static boolean waitUntil(long at) {
        try {
            long wait;
            while ((wait = at - AnimationClock.now()) > 0) {
                Thread.sleep(Math.min(wait, 5));
            }
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

        synchronized boolean isCancelled() {
        return cancelled;
    }

    synchronized void cancel() {
        cancelled = true;
        if (thread != null) {
            thread.interrupt();
        }
    }
}
