package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeInterface;
import com.codename1.ui.PeerComponent;

/// Android-only bridge behind SurfacesRemoteViewsScreenshotTest: renders the widget timeline
/// persisted by the real publish path (Surfaces.publish -> AndroidSurfaceBridge ->
/// CN1SurfaceStore) through the port's CN1SurfaceRenderer into RemoteViews, applies them to
/// real Android views and hands them back wrapped as a PeerComponent. Only the Android module
/// carries a functional implementation; the iOS stub reports unsupported and every other
/// platform has no implementation at all (NativeLookup.create returns null) -- the test gates
/// on the platform name before ever touching this interface.
public interface SurfacesRemoteViewsNative extends NativeInterface {
    /// Reads the timeline the app published for `kindId` from the on-device surface store,
    /// renders it through CN1SurfaceRenderer once in light and once in dark configuration,
    /// applies both RemoteViews to real views sized `widthPx` x `heightPx` and returns the
    /// two stacked vertically. Returns null when anything fails (details are logged).
    PeerComponent createWidgetView(String kindId, int widthPx, int heightPx);

    /// Renders the default layout of the given serialized timeline JSON (the
    /// SurfaceSerializer wire format) into RemoteViews and applies it to a real view without
    /// displaying it -- used to assert that Chronometer-backed timerDown/timerUp nodes lower
    /// without throwing, since their ticking makes them unfit for a deterministic screenshot.
    /// Returns "ok" on success or "error:" plus detail.
    String probeTimerRender(String timelineJson);
}
