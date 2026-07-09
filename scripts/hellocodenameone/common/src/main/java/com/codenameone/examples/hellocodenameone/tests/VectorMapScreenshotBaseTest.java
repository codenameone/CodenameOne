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

    // Generous: heavy first renders on a starved CI simulator have been observed
    // far beyond the old 9s cap; a healthy run exits after a few polls.
    private static final int MAX_WAIT_MS = 30000;
    private static final int POLL_MS = 150;

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        awaitTilesThenRun(parent, run, 0);
    }

    private void awaitTilesThenRun(final Form parent, final Runnable run, final int waitedMs) {
        if (isMapReady()) {
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
