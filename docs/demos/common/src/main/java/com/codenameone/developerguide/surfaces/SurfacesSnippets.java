package com.codenameone.developerguide.surfaces;

import com.codename1.surfaces.LiveActivity;
import com.codename1.surfaces.LiveActivityDescriptor;
import com.codename1.surfaces.SurfaceColor;
import com.codename1.surfaces.SurfaceColumn;
import com.codename1.surfaces.SurfaceDynamicText;
import com.codename1.surfaces.SurfaceFontWeight;
import com.codename1.surfaces.SurfaceImage;
import com.codename1.surfaces.SurfaceNode;
import com.codename1.surfaces.SurfaceProgress;
import com.codename1.surfaces.SurfaceRow;
import com.codename1.surfaces.SurfaceText;
import com.codename1.surfaces.SurfaceVector;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.WidgetKind;
import com.codename1.surfaces.WidgetSize;
import com.codename1.surfaces.WidgetTimeline;
import com.codename1.ui.Dialog;
import com.codename1.ui.Image;
import com.codename1.util.Callback;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Snippets that accompany the External Surfaces guide chapter. Each block
 * between the tag markers is included verbatim into the AsciiDoc.
 */
public class SurfacesSnippets {

    private static final int ACCENT_COLOR = 0xff6a1b9a;

    private LiveActivity activity;
    private Image courierAvatar;

    public void registerKinds() {
        // tag::registerKind[]
        Surfaces.registerWidgetKind(new WidgetKind("delivery_status")
                .setDisplayName("Delivery")
                .setDescription("Track your order")
                .addSupportedSize(WidgetSize.SMALL)
                .addSupportedSize(WidgetSize.MEDIUM));
        // end::registerKind[]
    }

    public void registerActionHandler() {
        // tag::actionHandler[]
        Surfaces.setActionHandler(evt -> {
            if ("open_order".equals(evt.getActionId())) {
                String orderId = (String) evt.getParams().get("orderId");
                showOrderForm(orderId);
            } else {
                Dialog.show("Surface Action", "Action " + evt.getActionId()
                        + " from " + evt.getSource()
                        + ", cold start: " + evt.isColdStart(), "OK", null);
            }
        });
        // end::actionHandler[]
    }

    public void publishDeliveryTimeline() {
        // tag::publishTimeline[]
        long now = System.currentTimeMillis();
        long eta = now + 4 * 60000L;
        WidgetTimeline timeline = new WidgetTimeline()
                .setContent(buildDeliveryLayout())
                .addEntry(new Date(now), deliveryState("Preparing your order", eta, 0.1f))
                .addEntry(new Date(now + 60000L), deliveryState("Out for delivery", eta, 0.4f))
                .addEntry(new Date(now + 3 * 60000L), deliveryState("Arriving now", eta, 0.9f))
                .addEntry(new Date(eta), deliveryState("Delivered", eta, 1f))
                .setReloadPolicy(WidgetTimeline.RELOAD_AT_END);
        Surfaces.publish("delivery_status", timeline);
        // end::publishTimeline[]
    }

    // tag::deliveryLayout[]
    private SurfaceNode buildDeliveryLayout() {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", "CN1-12345");
        return new SurfaceColumn().setSpacing(6).setPadding(12)
                .add(new SurfaceRow().setSpacing(10)
                        .add(new SurfaceImage(courierAvatar)
                                .setSize(40, 40).setCornerRadius(20))
                        .add(new SurfaceColumn().setSpacing(2).setWeight(1)
                                .add(new SurfaceText("${status}")
                                        .setFontSize(15)
                                        .setFontWeight(SurfaceFontWeight.SEMIBOLD)
                                        .setMaxLines(1))
                                .add(new SurfaceDynamicText(
                                        SurfaceDynamicText.STYLE_TIMER_DOWN, "eta")
                                        .setFontSize(24)
                                        .setFontWeight(SurfaceFontWeight.BOLD)
                                        .setColor(SurfaceColor.ACCENT))))
                .add(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                        .setValueState("progress")
                        .setColor(SurfaceColor.rgb(ACCENT_COLOR)))
                .setAction("open_order", params);
    }

    private Map<String, Object> deliveryState(String status, long eta, float progress) {
        Map<String, Object> state = new HashMap<>();
        state.put("status", status);
        state.put("eta", eta);
        state.put("progress", progress);
        return state;
    }
    // end::deliveryLayout[]

    // tag::clockFace[]
    private SurfaceNode buildClockFace() {
        SurfaceVector face = new SurfaceVector(200, 200)
                .fillEllipse(100, 100, 96, 96, SurfaceColor.rgb(0xfffafafa, 0xff1c1c1e))
                .strokeEllipse(100, 100, 96, 96, 4, SurfaceColor.rgb(ACCENT_COLOR));
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
                .line(100, 100, 100, 26, 5, SurfaceColor.rgb(ACCENT_COLOR))
                .endRotation()
                .fillEllipse(100, 100, 6, 6, SurfaceColor.rgb(ACCENT_COLOR));
        return face;
    }
    // end::clockFace[]

    public void publishClockTimeline() {
        // tag::clockTimeline[]
        WidgetTimeline timeline = new WidgetTimeline()
                .setContent(buildClockFace())
                .setReloadPolicy(WidgetTimeline.RELOAD_AT_END);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long minuteStart = c.getTimeInMillis();
        int baseMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        for (int m = 0; m < 60; m++) {
            int totalMinutes = baseMinutes + m;
            Map<String, Object> state = new HashMap<>();
            // clock convention: degrees, 0 = 12 o'clock, clockwise positive
            state.put("minuteAngle", totalMinutes % 60 * 6f);
            state.put("hourAngle", totalMinutes % 720 * 0.5f);
            timeline.addEntry(new Date(minuteStart + m * 60000L), state);
        }
        Surfaces.publish("analog_clock", timeline);
        // end::clockTimeline[]
    }

    public void startLiveActivity() {
        // tag::liveActivity[]
        long eta = System.currentTimeMillis() + 4 * 60000L;
        LiveActivityDescriptor descriptor = new LiveActivityDescriptor("delivery")
                .setContent(buildDeliveryLayout())
                .setCompactLeading(new SurfaceImage(courierAvatar)
                        .setSize(24, 24).setCornerRadius(12))
                .setCompactTrailing(new SurfaceDynamicText(
                        SurfaceDynamicText.STYLE_TIMER_DOWN, "eta")
                        .setFontSize(14).setColor(SurfaceColor.ACCENT))
                .setExpandedCenter(new SurfaceText("${status}")
                        .setFontSize(16).setFontWeight(SurfaceFontWeight.SEMIBOLD))
                .setExpandedBottom(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                        .setValueState("progress")
                        .setColor(SurfaceColor.rgb(ACCENT_COLOR)))
                .setTint(SurfaceColor.rgb(ACCENT_COLOR));
        activity = LiveActivity.start(descriptor,
                deliveryState("Out for delivery", eta, 0.25f));
        // end::liveActivity[]
    }

    public void updateAndEndLiveActivity() {
        long eta = System.currentTimeMillis() + 2 * 60000L;
        // tag::liveActivityUpdate[]
        activity.update(deliveryState("Arriving now", eta, 0.9f));
        // ... and when the delivery completes:
        activity.end(deliveryState("Delivered", System.currentTimeMillis(), 1f));
        // end::liveActivityUpdate[]
    }

    // tag::backgroundFetch[]
    public void performBackgroundFetch(long deadline, Callback<Boolean> onComplete) {
        // fetch fresh data, then re-publish the timeline
        publishDeliveryTimeline();
        onComplete.onSucess(Boolean.TRUE);
    }
    // end::backgroundFetch[]

    private void showOrderForm(String orderId) {
    }
}
