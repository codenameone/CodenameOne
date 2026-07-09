package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.MapSurface;
import com.codename1.maps.MapView;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;

/// Base for the bundled-tile vector-map screenshot tests. The tiles load
/// asynchronously, so a fixed settle occasionally fires before the basemap has
/// rendered and captures only the overlays on a blank background (a flaky
/// golden). Instead, poll the {@link MapView#isMapReady()} visible-tile
/// readiness probe and only capture once the engine has rendered the current
/// tile set (with a hard cap so a stuck load can't hang the suite).
///
/// Subclasses build their map in {@code runTest()} and assign it to
/// {@link #mapUnderTest} before showing the form.
public abstract class VectorMapScreenshotBaseTest extends BaseTest {

    /// The map whose tiles must finish loading before the capture; set by the
    /// subclass before {@code form.show()}.
    protected MapSurface mapUnderTest;

    private static final int MAX_WAIT_MS = 9000;
    private static final int POLL_MS = 150;

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        awaitTilesThenRun(parent, run, 0);
    }

    private void awaitTilesThenRun(final Form parent, final Runnable run, final int waitedMs) {
        boolean ready = isMapReady();
        if (ready || waitedMs >= MAX_WAIT_MS) {
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

    private boolean isMapReady() {
        if (mapUnderTest instanceof MapView) {
            return ((MapView) mapUnderTest).isMapReady();
        }
        return mapUnderTest != null && !mapUnderTest.isLoadingTiles();
    }
}
