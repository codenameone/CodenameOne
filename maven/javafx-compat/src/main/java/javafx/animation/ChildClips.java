/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package javafx.animation;

import java.util.List;

/// The children of a sequential or parallel transition, each placed on
/// the time of its parent: when it begins and how long it takes.
///
/// A child is before the playhead, under it or behind it. The parent
/// enters a child when the playhead reaches it, plays it while the
/// playhead is under it and leaves it at its far edge; moving backwards
/// does the same from the other side. A child reads what it starts from
/// the first time it is entered after its parent started, so a child
/// later in a sequence starts from what the earlier ones left.
final class ChildClips {

    private static final int BEFORE = 0;
    private static final int UNDER = 1;
    private static final int BEHIND = 2;

    private final Animation owner;
    private final boolean sequential;
    private Animation[] clips = new Animation[0];
    private double[] begin = new double[0];
    private double[] span = new double[0];
    private int[] state = new int[0];
    private boolean[] entered = new boolean[0];

    ChildClips(Animation owner, boolean sequential) {
        this.owner = owner;
        this.sequential = sequential;
    }

    /// Lays the children out and returns the length of the whole.
    double layout(List<Animation> children) {
        int count = 0;
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) != null) {
                count++;
            }
        }
        clips = new Animation[count];
        begin = new double[count];
        span = new double[count];
        state = new int[count];
        entered = new boolean[count];
        double length = 0;
        int at = 0;
        for (int i = 0; i < children.size(); i++) {
            Animation child = children.get(i);
            if (child == null) {
                continue;
            }
            clips[at] = child;
            span[at] = child.embeddedSpan();
            if (sequential) {
                begin[at] = length;
                length += span[at];
            } else {
                begin[at] = 0;
                length = Math.max(length, span[at]);
            }
            at++;
        }
        return length;
    }

    /// Sets where every child stands for a playhead at a time, without
    /// writing anything, and forgets what the children started from.
    void start(double time, double length, boolean capture) {
        for (int i = 0; i < clips.length; i++) {
            if (capture) {
                entered[i] = false;
            }
            double end = begin[i] + span[i];
            if (time <= 0) {
                state[i] = BEFORE;
            } else if (time >= length || end <= time) {
                state[i] = BEHIND;
            } else {
                // A child under the playhead is entered by the jump that
                // put the playhead there, after the ones behind it.
                state[i] = BEFORE;
            }
        }
    }

    /// Puts every child before or behind the playhead, as a new cycle
    /// finds them.
    void rewind(boolean forward) {
        for (int i = 0; i < clips.length; i++) {
            state[i] = forward ? BEFORE : BEHIND;
        }
    }

    private void enter(int i, boolean forward) {
        clips[i].embeddedEnter(forward, !entered[i]);
        entered[i] = true;
    }

    void playTo(double to, boolean forward) {
        Animation top = owner.root();
        int started = top.token;
        if (forward) {
            for (int i = 0; i < clips.length; i++) {
                double end = begin[i] + span[i];
                if (state[i] == BEFORE && (to > begin[i] || span[i] <= 0 && to >= begin[i])) {
                    enter(i, true);
                    state[i] = UNDER;
                }
                if (state[i] == UNDER) {
                    clips[i].embeddedPlayTo(Math.min(to - begin[i], span[i]));
                    if (top.token != started) {
                        return;
                    }
                    if (to >= end) {
                        state[i] = BEHIND;
                    }
                }
            }
        } else {
            for (int i = clips.length - 1; i >= 0; i--) {
                double end = begin[i] + span[i];
                if (state[i] == BEHIND && (to < end || span[i] <= 0 && to <= end)) {
                    enter(i, false);
                    state[i] = UNDER;
                }
                if (state[i] == UNDER) {
                    clips[i].embeddedPlayTo(Math.max(0, to - begin[i]));
                    if (top.token != started) {
                        return;
                    }
                    if (to <= begin[i]) {
                        state[i] = BEFORE;
                    }
                }
            }
        }
    }

    /// Shows the children as they are with the playhead at a time: the
    /// ones behind it at their end, the ones it has not reached at their
    /// start if they were ever played, and the one under it in between.
    void jumpTo(double time) {
        for (int i = 0; i < clips.length; i++) {
            double end = begin[i] + span[i];
            if (end <= time && time > 0) {
                if (!entered[i]) {
                    enter(i, true);
                }
                clips[i].embeddedJumpTo(span[i]);
                state[i] = BEHIND;
            }
        }
        for (int i = clips.length - 1; i >= 0; i--) {
            double end = begin[i] + span[i];
            if (end <= time && time > 0) {
                continue;
            }
            if (begin[i] >= time) {
                if (entered[i]) {
                    clips[i].embeddedJumpTo(0);
                }
                state[i] = BEFORE;
            } else {
                if (!entered[i]) {
                    enter(i, true);
                }
                clips[i].embeddedJumpTo(time - begin[i]);
                state[i] = UNDER;
            }
        }
    }
}
