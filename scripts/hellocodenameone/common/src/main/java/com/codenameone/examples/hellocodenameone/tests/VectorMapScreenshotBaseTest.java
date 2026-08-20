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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.MapSurface;
import com.codename1.maps.MapView;
import com.codename1.maps.NativeMap;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;

/// Base for the bundled-tile vector-map screenshot tests. The tiles load AND
/// render asynchronously, so a fixed settle occasionally fires before the
/// basemap has rendered and captures only the overlays on a blank background
/// (a flaky golden). Poll the {@link MapView#isMapReady()} visible-tile
/// readiness probe -- it actively computes the visible tile set and requests
/// missing tiles, so it is deterministic even before the first paint -- and
/// only capture once the engine has rendered the current tile set.
///
/// The probe is also only meaningful per viewport: it derives the visible tile set from the
/// component's current size, so consecutive "ready" answers count only while that size holds
/// still. A layout pass that enlarges the map after the count reaches two leaves the new area
/// unrendered at capture time -- seen on tvOS as a 4K frame with the basemap covering part of it
/// and the background colour covering the rest, and with no cap warning, because the probe had
/// been satisfied for the smaller viewport it was asked about.
///
/// isLoadingTiles() alone is NOT a sufficient readiness signal: tiles are
/// requested lazily by paint, so the in-flight set can be EMPTY between
/// request batches while most of the viewport is still unrendered (observed
/// as a build-ios capture with only the first tile batch drawn).
///
/// The wait has a hard cap so a stuck load can't hang the suite -- logged
/// loudly, since a capture at the cap is a guaranteed mismatch on whichever
/// leg is slow.
///
/// Subclasses build their map in {@code runTest()} and assign it to
/// {@link #mapUnderTest} before showing the form.
public abstract class VectorMapScreenshotBaseTest extends BaseTest {

    /// The map whose tiles must finish loading before the capture; set by the
    /// subclass before {@code form.show()}.
    protected MapSurface mapUnderTest;

    // Minimum elapsed before trusting the probe: the first isMapReady() can fire
    // BEFORE the host's final layout pass -- which may then reset the pixel ratio,
    // bump the engine generation and CLEAR the rendered-tile cache, so a capture
    // taken off that early "ready" catches the reload mid-flight (observed on the
    // Android leg: capture 1.5s after test start with one visible tile beige).
    private static final int MIN_SETTLE_MS = 1500;
    // Generous: heavy first renders on a starved CI simulator have been observed
    // far beyond the old 9s cap; a healthy run exits after a few polls.
    //
    // MUST stay strictly below the runner's per-test budget for these tests, or
    // the "capture anyway" fallback below is unreachable. It used to exceed both
    // defaults -- equal to the 30s native one, triple the 10s HTML5 one -- so the
    // runner declared its own timeout first and the fallback never once fired: a
    // map that was slow to report ready failed with no screenshot at all,
    // instead of a screenshot showing what it did render.
    //
    // Package-visible on purpose: Cn1ssDeviceRunner derives the budget for every
    // VectorMapScreenshotBaseTest from this value rather than restating it, so
    // tuning the cap here cannot silently re-create that inversion.
    static final int MAX_WAIT_MS = 30000;
    private static final int POLL_MS = 150;
    // The probe must hold across consecutive polls: a single true can sit right
    // before a generation clear (see MIN_SETTLE_MS note).
    private static final int STABLE_POLLS = 2;

    private int consecutiveReady;

    /// The map's size when the readiness probe last answered, so a probe taken against one
    /// viewport is not credited to another.
    private int lastWidth = -1;

    private int lastHeight = -1;

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        consecutiveReady = 0;
        awaitTilesThenRun(parent, run, 0);
    }

    private void awaitTilesThenRun(final Form parent, final Runnable run, final int waitedMs) {
        // The size the probe is about to answer for. isMapReady() computes the visible tile set
        // from the component's current width and height, so a run of "ready" polls only means
        // anything if they were all asked about the same viewport: a layout pass that widens the
        // map afterwards leaves tiles for the new area unrendered while the count already stands
        // at two. That is what a tvOS capture showed -- the basemap drawn across part of a 4K
        // viewport and the rest left at the background colour, with no cap warning, because the
        // probe had genuinely been satisfied for the viewport it was asked about.
        int width = mapWidth();
        int height = mapHeight();
        if (width != lastWidth || height != lastHeight) {
            consecutiveReady = 0;
            lastWidth = width;
            lastHeight = height;
        }
        if (isMapReady()) {
            consecutiveReady++;
        } else {
            consecutiveReady = 0;
        }
        if (consecutiveReady >= STABLE_POLLS && waitedMs >= MIN_SETTLE_MS) {
            run.run();
            return;
        }
        if (waitedMs >= MAX_WAIT_MS) {
            System.out.println("CN1SS:WARN:test=" + getClass().getSimpleName()
                    + " map not fully rendered after " + waitedMs + "ms; capturing anyway");
            run.run();
            return;
        }
        UITimer.timer(POLL_MS, false, parent, new Runnable() {
            @Override
            public void run() {
                awaitTilesThenRun(parent, run, waitedMs + POLL_MS);
            }
        });
    }

    private int mapWidth() {
        return mapUnderTest instanceof com.codename1.ui.Component
                ? ((com.codename1.ui.Component) mapUnderTest).getWidth() : -1;
    }

    private int mapHeight() {
        return mapUnderTest instanceof com.codename1.ui.Component
                ? ((com.codename1.ui.Component) mapUnderTest).getHeight() : -1;
    }

    private boolean isMapReady() {
        if (mapUnderTest instanceof MapView) {
            return ((MapView) mapUnderTest).isMapReady();
        }
        if (mapUnderTest instanceof NativeMap) {
            return ((NativeMap) mapUnderTest).isMapReady();
        }
        return mapUnderTest != null && !mapUnderTest.isLoadingTiles();
    }
}
