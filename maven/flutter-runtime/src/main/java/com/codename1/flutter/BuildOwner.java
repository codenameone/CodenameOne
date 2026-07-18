package com.codename1.flutter;

import com.codename1.flutter.rendering.RenderHost;
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

    void flushBuild() {
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
        for (RenderHost h : affectedHosts) {
            h.revalidate();
        }
    }
}
