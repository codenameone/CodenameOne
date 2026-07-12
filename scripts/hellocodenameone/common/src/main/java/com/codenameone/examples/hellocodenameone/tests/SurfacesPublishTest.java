package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.surfaces.SurfaceColor;
import com.codename1.surfaces.SurfaceColumn;
import com.codename1.surfaces.SurfaceText;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.WidgetKind;
import com.codename1.surfaces.WidgetSize;
import com.codename1.surfaces.WidgetTimeline;
import com.codename1.ui.Display;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// External-surfaces publish contract across the whole platform matrix: registering a widget
/// kind, publishing a tiny timeline, reloading and querying the installed count must be safe
/// no-ops where the platform has no SurfaceBridge (HTML5, TV/watch) and must not throw where a
/// bridge exists (JavaSE simulator preview, Android provider, iOS app-group persist, the
/// Windows/Linux desktop widget windows). Deliberately asserts NO platform-specific support
/// values -- only the invariants every platform shares. The registered kind id mirrors the
/// project's surfaces.json build-time manifest. Assertion-only test, no screenshot.
public class SurfacesPublishTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            // support probes must never throw, whatever they answer
            boolean widgets = Surfaces.areWidgetsSupported();
            System.out.println("CN1SS:INFO:test=SurfacesPublishTest widgets_supported=" + widgets
                    + " platform=" + Display.getInstance().getPlatformName());

            // register the kind declared in surfaces.json (the build-time manifest)
            Surfaces.registerWidgetKind(new WidgetKind("cn1ss_status")
                    .setDisplayName("CN1SS Status")
                    .setDescription("Surfaces suite status widget")
                    .addSupportedSize(WidgetSize.SMALL)
                    .addSupportedSize(WidgetSize.MEDIUM));
            List<WidgetKind> kinds = Surfaces.getRegisteredKinds();
            boolean found = false;
            for (WidgetKind k : kinds) {
                if ("cn1ss_status".equals(k.getId())) {
                    found = true;
                    assertEqual("CN1SS Status", k.getDisplayName(), "registered kind name");
                }
            }
            assertBool(found, "registered kind is listed");

            // re-registering the same id replaces rather than duplicates
            Surfaces.registerWidgetKind(new WidgetKind("cn1ss_status")
                    .setDisplayName("CN1SS Status"));
            int count = 0;
            for (WidgetKind k : Surfaces.getRegisteredKinds()) {
                if ("cn1ss_status".equals(k.getId())) {
                    count++;
                }
            }
            assertEqual(1, count, "re-registered kind is not duplicated");

            // publishing a tiny timeline must not throw anywhere: it is a no-op without a
            // bridge and persists + re-renders where one exists
            Map<String, Object> state = new HashMap<String, Object>();
            state.put("status", "cn1ss");
            Surfaces.publish("cn1ss_status", new WidgetTimeline()
                    .setContent(new SurfaceColumn()
                            .add(new SurfaceText("cn1ss ${status}")
                                    .setColor(SurfaceColor.LABEL)))
                    .addEntry(new Date(), state));

            // installed count is always a non-negative number
            int installed = Surfaces.getInstalledWidgetCount("cn1ss_status");
            assertBool(installed >= 0, "installed widget count is non-negative but was "
                    + installed);

            // reload of all kinds (null) and of an unknown kind must be safe everywhere
            Surfaces.reloadWidgets(null);
            Surfaces.reloadWidgets("cn1ss_no_such_kind");
            assertBool(Surfaces.getInstalledWidgetCount("cn1ss_no_such_kind") >= 0,
                    "unknown kind count is non-negative");

            // live activity lifecycle: start/update/end must be safe on every platform.
            // Where unsupported (or the platform refuses, e.g. the iOS simulator test
            // harness) start returns an inert handle whose methods are documented
            // no-ops; where supported (Android ongoing notification, desktop pill)
            // the full cycle must run without throwing. No support value is asserted.
            boolean activities = com.codename1.surfaces.LiveActivity.isSupported();
            System.out.println("CN1SS:INFO:test=SurfacesPublishTest activities_supported="
                    + activities);
            Map<String, Object> laState = new HashMap<String, Object>();
            laState.put("status", "started");
            com.codename1.surfaces.LiveActivity activity =
                    com.codename1.surfaces.LiveActivity.start(
                            new com.codename1.surfaces.LiveActivityDescriptor("cn1ss")
                                    .setContent(new SurfaceColumn()
                                            .add(new SurfaceText("cn1ss ${status}")
                                                    .setColor(SurfaceColor.LABEL)))
                                    .setCompactLeading(new SurfaceText("cn1ss"))
                                    .setCompactTrailing(new SurfaceText("${status}")),
                            laState);
            assertBool(activity != null, "start returns a handle even when unsupported");
            laState.put("status", "updated");
            activity.update(laState);
            laState.put("status", "done");
            activity.end(laState, true);
            assertBool(!activity.isActive(), "activity is inactive after end");
            // ended and inert handles tolerate further calls
            activity.update(laState);
            activity.end(null);
        } catch (Throwable t) {
            fail("Surfaces publish contract failed: " + t);
            return false;
        }
        done();
        return true;
    }
}
