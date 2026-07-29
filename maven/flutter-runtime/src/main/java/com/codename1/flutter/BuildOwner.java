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
                + ",\"hot\":" + RenderElement.hotLayoutClasses(6) + "}";
    }

    void flushBuild() {
        long started = traceFrames ? System.currentTimeMillis() : 0;
        int rebuilt = 0;
        flushScheduled = false;
        Set<RenderHost> affectedHosts = new HashSet<RenderHost>();
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
        }
        long built = traceFrames ? System.currentTimeMillis() : 0;
        for (RenderHost h : affectedHosts) {
            h.revalidate();
        }
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
