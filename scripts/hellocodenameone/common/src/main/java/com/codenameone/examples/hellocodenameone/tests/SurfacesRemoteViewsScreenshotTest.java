package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.surfaces.SurfaceAlignment;
import com.codename1.surfaces.SurfaceBox;
import com.codename1.surfaces.SurfaceColor;
import com.codename1.surfaces.SurfaceColumn;
import com.codename1.surfaces.SurfaceDynamicText;
import com.codename1.surfaces.SurfaceFontWeight;
import com.codename1.surfaces.SurfaceProgress;
import com.codename1.surfaces.SurfaceRow;
import com.codename1.surfaces.SurfaceSerializer;
import com.codename1.surfaces.SurfaceSpacer;
import com.codename1.surfaces.SurfaceText;
import com.codename1.surfaces.SurfaceVector;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.WidgetKind;
import com.codename1.surfaces.WidgetSize;
import com.codename1.surfaces.WidgetTimeline;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.hellocodenameone.SurfacesRemoteViewsNative;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/// ANDROID-ONLY end-to-end validation of the real RemoteViews widget lowering. The shared
/// SurfaceRasterizer already has a visual baseline on every platform
/// (SurfacesRasterizerScreenshotTest); this test covers the code the home-screen widget
/// actually runs on Android: the timeline goes through the REAL publish path
/// (Surfaces.publish -> AndroidSurfaceBridge -> CN1SurfaceStore on disk), is read back and
/// rendered through CN1SurfaceRenderer into RemoteViews composed from the build's pre-baked
/// cn1_surface_* layout resources, applied to real views (RemoteViews.apply) in light and
/// dark configuration, and displayed in the form through a native PeerComponent -- the
/// Android screenshot path (PixelCopy of the window) captures native peers.
///
/// Determinism: the descriptor mirrors SurfacesRasterizerScreenshotTest (same pinned NOW,
/// colors and overall structure) with the nodes that tick or read the wall clock on Android
/// swapped for static equivalents: the Chronometer-backed timerDown/timerUp become static
/// texts, the date-interval circular progress becomes an explicit value, and the relative
/// dynamic text is dropped (DateUtils formats it against the real clock). A dyn `date` node
/// stays -- it renders as static text from the pinned date on Android -- and a state-driven
/// vector clock face covers the in-process bitmap vec lowering. The Chronometer path is
/// asserted separately without a screenshot: probeTimerRender renders + applies a
/// timerDown/timerUp timeline and reports failures.
///
/// Everywhere but Android the test self-skips (CN1SS SKIPPED log, no screenshot, no golden).
public class SurfacesRemoteViewsScreenshotTest extends BaseTest {
    private static final String NAME = "SurfacesRemoteViews";
    /// Same pinned clock as SurfacesRasterizerScreenshotTest: 2025-06-15T15:06:40Z.
    private static final long NOW = 1750000000000L;
    /// The kind declared in the suite's surfaces.json build-time manifest.
    private static final String KIND = "cn1ss_status";

    @Override
    public boolean runTest() {
        if (!"and".equals(Display.getInstance().getPlatformName())) {
            System.out.println("CN1SS:INFO:test=" + NAME
                    + " status=SKIPPED reason=android-only-remoteviews-lowering platform="
                    + Display.getInstance().getPlatformName());
            done();
            return true;
        }
        SurfacesRemoteViewsNative bridge = NativeLookup.create(SurfacesRemoteViewsNative.class);
        if (bridge == null || !bridge.isSupported()) {
            fail("SurfacesRemoteViewsNative bridge is missing on Android");
            return false;
        }
        PeerComponent peer;
        try {
            Surfaces.registerWidgetKind(new WidgetKind(KIND)
                    .setDisplayName("CN1SS Status")
                    .setDescription("Surfaces suite status widget")
                    .addSupportedSize(WidgetSize.SMALL)
                    .addSupportedSize(WidgetSize.MEDIUM));
            // the REAL publish path: serializer -> AndroidSurfaceBridge -> CN1SurfaceStore
            Surfaces.publish(KIND, new WidgetTimeline()
                    .setContent(buildDeterministicLayout())
                    .addEntry(new Date(NOW - 1000L), publishedState()));

            // Chronometer lowering probe, asserted without a screenshot (countdowns tick)
            String probe = bridge.probeTimerRender(SurfaceSerializer.serializeTimeline(KIND,
                    buildTimerTimeline(), new HashMap<String, byte[]>()));
            assertEqual("ok", probe, "timerDown/timerUp RemoteViews probe");

            int dw = Display.getInstance().getDisplayWidth();
            int widthPx = dw * 4 / 5;
            int heightPx = widthPx * 9 / 16;
            peer = bridge.createWidgetView(KIND, widthPx, heightPx);
        } catch (Throwable t) {
            fail("Surfaces RemoteViews setup failed: " + t);
            return false;
        }
        if (peer == null) {
            fail("createWidgetView returned null; see the CN1SS logcat entries");
            return false;
        }
        Form form = createForm(NAME, new BorderLayout(), NAME);
        form.add(BorderLayout.CENTER, peer);
        form.show();
        return true;
    }

    /// The rasterizer test's descriptor with its wall-clock-dependent nodes replaced by
    /// static equivalents so the RemoteViews render is bit-stable across runs.
    private SurfaceColumn buildDeterministicLayout() {
        SurfaceColumn root = new SurfaceColumn().setSpacing(6);
        root.setPadding(12);
        root.setBackground(SurfaceColor.rgb(0xfff2f5f8, 0xff101820));
        root.setCornerRadius(20);
        root.add(new SurfaceText("CN1 Surfaces")
                        .setFontSize(18)
                        .setFontWeight(SurfaceFontWeight.SEMIBOLD)
                        .setColor(SurfaceColor.rgb(0xff16324f, 0xffdce6f0))
                        .setAlignment(SurfaceAlignment.LEADING))
                .add(new SurfaceRow()
                        .setSpacing(6)
                        .add(new SurfaceText("ETA")
                                .setFontSize(12)
                                .setColor(SurfaceColor.rgb(0xff5a6b7c, 0xff9aa8b6)))
                        // static stand-in for the ticking countdown (probed separately)
                        .add(new SurfaceText("${etaText}")
                                .setFontSize(16)
                                .setFontWeight(SurfaceFontWeight.BOLD)
                                .setColor(SurfaceColor.rgb(0xffb03030, 0xffffb4a0)))
                        .add(new SurfaceSpacer())
                        // renders as static text computed from the pinned date on Android
                        .add(new SurfaceDynamicText(SurfaceDynamicText.STYLE_DATE,
                                new Date(NOW))
                                .setFontSize(12)
                                .setColor(SurfaceColor.rgb(0xff3a3a3a, 0xffc0c0c0))))
                .add(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                        .setValueState("progress")
                        .setColor(SurfaceColor.rgb(0xff2e7d32, 0xff81c784)))
                .add(new SurfaceRow()
                        .setSpacing(8)
                        // explicit value instead of the rasterizer test's date interval
                        // (frozen against the real clock on Android); falls back to a
                        // linear bar per the RemoteViews LCD contract
                        .add(new SurfaceProgress(SurfaceProgress.STYLE_CIRCULAR)
                                .setValue(0.3f)
                                .setColor(SurfaceColor.rgb(0xffe65100, 0xffffb74d)))
                        .add(new SurfaceText("${status}")
                                .setFontSize(12)
                                .setColor(SurfaceColor.rgb(0xff1c1c1e, 0xffe8e8e8)))
                        .add(new SurfaceSpacer())
                        // state-keyed rotation groups exercise the in-process vec bitmap
                        .add(buildClockFace().setSize(48, 48)))
                .add(new SurfaceBox()
                        .add(new SurfaceText("Open order")
                                .setFontSize(12)
                                .setFontWeight(SurfaceFontWeight.SEMIBOLD)
                                .setColor(SurfaceColor.rgb(0xffffffff)))
                        .setBackground(SurfaceColor.rgb(0xff4a90d9, 0xff2a5a89))
                        .setCornerRadius(10)
                        .setSize(0, 36)
                        .setAction("open", actionParams()));
        return root;
    }

    /// A fixed 4:15 analog clock face: dial, twelve ticks, state-driven hour/minute hands.
    private SurfaceVector buildClockFace() {
        SurfaceVector face = new SurfaceVector(200, 200)
                .fillEllipse(100, 100, 96, 96, SurfaceColor.rgb(0xfffafafa, 0xff1c1c1e))
                .strokeEllipse(100, 100, 96, 96, 4, SurfaceColor.rgb(0xff4a90d9, 0xff2a5a89));
        for (int hour = 0; hour < 12; hour++) {
            boolean quarter = hour % 3 == 0;
            face.beginRotation(hour * 30f, 100, 100)
                    .line(100, 10, 100, quarter ? 24 : 18, quarter ? 5 : 3, SurfaceColor.LABEL)
                    .endRotation();
        }
        face.beginRotation("hourAngle", 100, 100)
                .line(100, 100, 100, 52, 8, SurfaceColor.LABEL)
                .endRotation()
                .beginRotation("minuteAngle", 100, 100)
                .line(100, 100, 100, 26, 5, SurfaceColor.rgb(0xffb03030, 0xffffb4a0))
                .endRotation()
                .fillEllipse(100, 100, 6, 6, SurfaceColor.rgb(0xff4a90d9, 0xff2a5a89));
        return face;
    }

    /// A tiny countdown timeline for the no-screenshot Chronometer probe.
    private WidgetTimeline buildTimerTimeline() {
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("eta", Long.valueOf(NOW + 754000L));
        return new WidgetTimeline()
                .setContent(new SurfaceColumn()
                        .add(new SurfaceDynamicText(SurfaceDynamicText.STYLE_TIMER_DOWN, "eta")
                                .setFontSize(16))
                        .add(new SurfaceDynamicText(SurfaceDynamicText.STYLE_TIMER_UP,
                                new Date(NOW - 90000L))
                                .setFontSize(12)))
                .addEntry(new Date(NOW - 1000L), state);
    }

    private Map<String, Object> publishedState() {
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("status", "On the way");
        state.put("etaText", "12:34");
        state.put("progress", Double.valueOf(0.62));
        // 4:15 in the clock convention: degrees, 0 = 12 o'clock, clockwise positive
        state.put("hourAngle", Double.valueOf(127.5));
        state.put("minuteAngle", Double.valueOf(90));
        return state;
    }

    private Map<String, Object> actionParams() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("orderId", Integer.valueOf(7));
        return params;
    }
}
