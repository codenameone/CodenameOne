package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.MapSurface;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;

/// Base for the bundled-tile vector-map screenshot tests. The tiles load
/// asynchronously, so a fixed settle occasionally fires before the basemap has
/// rendered and captures only the overlays on a blank background (a flaky
/// golden). Instead, poll {@link MapSurface#isLoadingTiles()} and only capture
/// once the engine has no tiles in flight (with a minimum settle so the final
/// tile paint lands, and a hard cap so a stuck load can't hang the suite).
///
/// Subclasses build their map in {@code runTest()} and assign it to
/// {@link #mapUnderTest} before showing the form.
public abstract class VectorMapScreenshotBaseTest extends BaseTest {

    /// The map whose tiles must finish loading before the capture; set by the
    /// subclass before {@code form.show()}.
    protected MapSurface mapUnderTest;

    private static final int MIN_SETTLE_MS = 1500;
    private static final int MAX_WAIT_MS = 9000;
    private static final int POLL_MS = 150;

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        awaitTilesThenRun(parent, run, 0);
    }

    private void awaitTilesThenRun(final Form parent, final Runnable run, final int waitedMs) {
        boolean loading = mapUnderTest != null && mapUnderTest.isLoadingTiles();
        if ((!loading && waitedMs >= MIN_SETTLE_MS) || waitedMs >= MAX_WAIT_MS) {
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
}
