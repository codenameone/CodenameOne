/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import com.codename1.flutter.rendering.RenderHost;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks dirty elements and coalesces their rebuilds into one flush per
 * frame, scheduled via {@code CN.callSerially}. Elements rebuild parents
 * before children (depth order) so a parent rebuild that already updated a
 * dirty child doesn't rebuild it twice. After the flush every affected host
 * container is revalidated, which re-runs the Flutter constraint pass through
 * {@code FlutterRootLayout}.
 */
public class BuildOwner {

    private final List<Element> dirtyElements = new ArrayList<Element>();
    private boolean flushScheduled;

    private static final Comparator<Element> BY_DEPTH = new Comparator<Element>() {
        @Override
        public int compare(Element a, Element b) {
            return a.depth - b.depth;
        }
    };

    /**
     * Adds a dirty element and schedules a coalesced flush on the EDT. When
     * no Display is initialized (headless unit tests) nothing is scheduled;
     * tests drive {@link #flushSync()} directly.
     */
    public void scheduleBuildFor(Element element) {
        FlutterUI.assertEdt();
        if (!dirtyElements.contains(element)) {
            dirtyElements.add(element);
        }
        if (!flushScheduled) {
            flushScheduled = true;
            if (Display.isInitialized()) {
                CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        flushBuild();
                    }
                });
            }
        }
    }

    /**
     * Test hook: flushes the dirty list synchronously.
     */
    public void flushSync() {
        flushBuild();
    }

    /// Frames slower than this are worth knowing about: at 60fps the whole budget is 16ms,
    /// so a build flush that costs more than this cannot keep up with a finger.
    private static final long SLOW_FRAME_MS = 16;

    private static boolean traceFrames;
    private static long framesTraced;
    private static long buildMsTotal;
    private static long revalidateMsTotal;
    private static long rebuiltTotal;
    private static long worstFrameMs;

    /// Starts or stops recording what each build flush costs. Off by default - this is a
    /// diagnostic for "the UI feels slow", which is a question about where the frame went,
    /// not about whether anything is broken.
    public static void traceFrames(boolean on) {
        traceFrames = on;
        if (on) {
            framesTraced = 0;
            buildMsTotal = 0;
            revalidateMsTotal = 0;
            rebuiltTotal = 0;
            worstFrameMs = 0;
            RenderElement.resetLayoutCounters();
        }
        com.codename1.flutter.animation.AnimationTrace.trace(on);
    }

    /// What the traced frames cost, as a one-line summary.
    public static String frameStats() {
        return "{\"frames\":" + framesTraced
                + ",\"elementsRebuilt\":" + rebuiltTotal
                + ",\"buildMs\":" + buildMsTotal
                + ",\"revalidateMs\":" + revalidateMsTotal
                + ",\"worstFrameMs\":" + worstFrameMs
                + ",\"layoutCalls\":" + RenderElement.layoutCalls
                + ",\"layoutHits\":" + RenderElement.layoutHits
                + ",\"missDirty\":" + RenderElement.layoutMissDirty
                + ",\"missConstraints\":" + RenderElement.layoutMissConstraints
                + "," + com.codename1.flutter.animation.AnimationTrace.stats()
                + ",\"hot\":" + RenderElement.hotLayoutClasses(6) + "}";
    }

    /// Adds the hosts owned by anything inside {@code e}'s subtree to {@code out}.
    private static void collectNestedHosts(Element e, final Set<RenderHost> out) {
        e.visitChildren(new dart.runtime.Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element child) {
                if (child == null) {
                    return;
                }
                if (child.host() != null) {
                    out.add(child.host());
                }
                collectNestedHosts(child, out);
            }
        });
    }

    /// How many times we look for hosts that appeared during the pass we just ran. One
    /// extra round covers a subtree that builds late; the bound stops a tree that
    /// somehow keeps producing hosts from looping here forever.
    private static final int HOST_SETTLE_ROUNDS = 3;

    /**
     * Lays out the hosts that did not exist when this flush chose what to lay out.
     *
     * <p>The host set is collected from the rebuilt subtrees BEFORE anything is measured,
     * which assumes every host exists by then. Not all do. A LayoutBuilder sits out a
     * speculative measurement pass and inflates its subtree on a later one -- during the
     * very revalidate above -- and each thing it inflates can bring a host of its own: an
     * effect pane, a scroll pane. Those hosts were never in the set, so they were never
     * laid out, and the elements inside them kept the offsets their parent had stored
     * while they still measured zero.</p>
     *
     * <p>On screen that is a page with nothing on it. Coming back from Reply's search page
     * rebuilt two elements and found seven hosts; the mail list underneath them owns
     * thirty, one per card, all created while those seven were being laid out. Every card
     * ended up at the list's origin inside a pane of zero size, and a zero-sized pane
     * clips what it hosts, so the list was invisible rather than merely stacked. It stayed
     * that way until some unrelated rebuild -- opening the mailbox drawer -- happened to
     * find all thirty and lay them out.</p>
     */
    private static void revalidateHostsBornDuringLayout(List<Element> rebuiltRoots,
            Set<RenderHost> alreadyDone) {
        for (int round = 0; round < HOST_SETTLE_ROUNDS; round++) {
            Set<RenderHost> found = new HashSet<RenderHost>();
            for (Element e : rebuiltRoots) {
                if (e.mounted) {
                    if (e.host() != null) {
                        found.add(e.host());
                    }
                    collectNestedHosts(e, found);
                }
            }
            found.removeAll(alreadyDone);
            if (found.isEmpty()) {
                return;
            }
            alreadyDone.addAll(found);
            for (RenderHost h : found) {
                h.revalidate();
            }
        }
    }

    void flushBuild() {
        long started = traceFrames ? System.currentTimeMillis() : 0;
        int rebuilt = 0;
        flushScheduled = false;
        Set<RenderHost> affectedHosts = new HashSet<RenderHost>();
        List<Element> rebuiltRoots = new ArrayList<Element>();
        int guard = 0;
        while (!dirtyElements.isEmpty()) {
            if (++guard > 10000) {
                dirtyElements.clear();
                throw new IllegalStateException("Flutter build did not settle; an element keeps marking itself dirty during build");
            }
            Collections.sort(dirtyElements, BY_DEPTH);
            Element e = dirtyElements.remove(0);
            if (!e.mounted || !e.dirty) {
                continue;
            }
            e.rebuild();
            rebuilt++;
            // Invalidate cached layout up this branch so the coming
            // revalidate recomputes it.
            for (Element a = e; a != null; a = a.parent) {
                if (a instanceof RenderElement) {
                    ((RenderElement) a).markNeedsLayout();
                    break;
                }
            }
            if (e.host != null) {
                affectedHosts.add(e.host);
            }
            // ...and every NESTED host inside what was just rebuilt. An element records the
            // host it was mounted into, but a rebuilt subtree can contain scroll panes and
            // effect panes that carry hosts of their own, and those hold the components.
            // Revalidating only the outer host leaves a nested one holding children it
            // never laid out - which renders as a page whose scaffold is present and whose
            // contents have simply vanished, with no error anywhere.
            collectNestedHosts(e, affectedHosts);
            rebuiltRoots.add(e);
        }
        long built = traceFrames ? System.currentTimeMillis() : 0;
        for (RenderHost h : affectedHosts) {
            h.revalidate();
        }
        revalidateHostsBornDuringLayout(rebuiltRoots, affectedHosts);
        if (traceFrames) {
            long now = System.currentTimeMillis();
            long buildMs = built - started;
            long revalidateMs = now - built;
            long frameMs = now - started;
            framesTraced++;
            rebuiltTotal += rebuilt;
            buildMsTotal += buildMs;
            revalidateMsTotal += revalidateMs;
            worstFrameMs = Math.max(worstFrameMs, frameMs);
            if (frameMs >= SLOW_FRAME_MS) {
                Log.p("Flutter frame: " + frameMs + "ms (build " + buildMs + "ms for "
                        + rebuilt + " elements, revalidate " + revalidateMs + "ms across "
                        + affectedHosts.size() + " host(s))");
            }
        }
    }
}
