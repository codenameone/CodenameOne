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
package com.codename1.samples;

import com.codename1.background.BackgroundFetch;
import com.codename1.surfaces.LiveActivity;
import com.codename1.surfaces.LiveActivityDescriptor;
import com.codename1.surfaces.SurfaceActionEvent;
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
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UITimer;
import com.codename1.util.Callback;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates the external surfaces API ({@code com.codename1.surfaces}): one declarative model
 * for home-screen widgets and live activities. The sample is a delivery tracker -- it publishes a
 * widget timeline that advances from "preparing" to "delivered" over four minutes with an
 * OS-animated countdown to the ETA, and runs a live activity with Dynamic Island regions that the
 * app updates as the delivery progresses. A second widget kind is an analog clock built with
 * {@link SurfaceVector}: the vector face publishes once and 60 per-minute timeline entries drive
 * the hour/minute hand rotation angles through state keys.
 *
 * <p>Where to see the surfaces: in the simulator open "Widgets" &gt; "Widgets Preview" (the mock
 * Dynamic Island at the bottom shows the live activity). On an Android device add the "Delivery"
 * widget from the launcher's widget gallery; on iOS add it from the home-screen widget gallery and
 * watch the live activity on the lock screen / Dynamic Island (iPhone 14 Pro or later). Surfaces
 * render while the app process is dead; tapping one opens the app and delivers the action id to
 * the handler registered in {@link #init}.</p>
 *
 * <p>The build wires the native plumbing (iOS WidgetKit extension, Android widget receivers)
 * automatically because this sample references {@code com.codename1.surfaces}. Widget kinds must
 * also be declared at build time in the {@code surfaces.json} project resource -- see the copy in
 * this sample's directory and the {@code com.codename1.surfaces} package documentation.</p>
 *
 * <p>Run with {@code -Dcn1.surfaces.autodemo=true} to auto-publish the timeline, auto-start the
 * live activity and auto-open the simulator Widgets preview two seconds after startup (used for
 * automated verification).</p>
 */
public class SurfacesSample implements BackgroundFetch {
    private static final String KIND_DELIVERY = "delivery_status";
    private static final String KIND_CLOCK = "analog_clock";
    private static final int ACCENT_COLOR = 0xff6a1b9a;

    private Form current;
    private Form mainForm;
    private Resources theme;
    private LiveActivity activity;
    private long activityEta;
    private float activityProgress;
    private Label installedLabel;
    private Label activityLabel;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        // Declare the widget kind. This mirrors the surfaces.json build-time manifest (the
        // platform widget galleries are compiled into the native app) and belongs in init() so
        // the kind is known before anything publishes.
        Surfaces.registerWidgetKind(new WidgetKind(KIND_DELIVERY)
                .setDisplayName("Delivery")
                .setDescription("Track your order")
                .addSupportedSize(WidgetSize.SMALL)
                .addSupportedSize(WidgetSize.MEDIUM));
        Surfaces.registerWidgetKind(new WidgetKind(KIND_CLOCK)
                .setDisplayName("Analog Clock")
                .setDescription("A vector clock face")
                .addSupportedSize(WidgetSize.SMALL));
        // One handler receives every surface tap on the EDT -- including the tap that
        // cold-started the app (queued until this registration, flagged isColdStart()).
        Surfaces.setActionHandler(evt -> {
            if ("open_order".equals(evt.getActionId())) {
                showOrderForm(evt);
            } else {
                Dialog.show("Surface Action", "Action \"" + evt.getActionId() + "\" from "
                        + evt.getSource() + "\nparams: " + evt.getParams()
                        + "\ncold start: " + evt.isColdStart(), "OK", null);
            }
        });
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Display d = Display.getInstance();
        // Registers this app for periodic background wakeups; performBackgroundFetch below
        // re-publishes the widget timeline with fresh data. Widgets themselves render and tick
        // without the app, this is only the data-refresh hook.
        d.setPreferredBackgroundFetchInterval(300);

        Form f = new Form("Surfaces Sample", BoxLayout.y());
        f.add(new Label("Widgets supported: " + Surfaces.areWidgetsSupported()));
        f.add(new Label("Live activities supported: " + LiveActivity.isSupported()));
        f.add(new Label("Background fetch supported: " + d.isBackgroundFetchSupported()));
        installedLabel = new Label("Installed delivery widgets: "
                + Surfaces.getInstalledWidgetCount(KIND_DELIVERY));
        f.add(installedLabel);
        activityLabel = new Label("Live activity: not started");
        f.add(activityLabel);

        Button publish = new Button("Publish widget timeline");
        publish.addActionListener(e -> {
            publishDeliveryTimeline();
            installedLabel.setText("Installed delivery widgets: "
                    + Surfaces.getInstalledWidgetCount(KIND_DELIVERY));
            f.revalidate();
        });
        f.add(publish);

        Button publishClock = new Button("Publish clock widget");
        publishClock.addActionListener(e -> publishClockTimeline());
        f.add(publishClock);

        Button startActivity = new Button("Start Live Activity");
        startActivity.addActionListener(e -> startDeliveryActivity());
        f.add(startActivity);

        Button advance = new Button("Advance state");
        advance.addActionListener(e -> advanceActivity());
        f.add(advance);

        Button end = new Button("End activity");
        end.addActionListener(e -> endActivity());
        f.add(end);

        mainForm = f;
        f.show();

        if ("true".equals(System.getProperty("cn1.surfaces.autodemo"))) {
            // Demo/CI mode: publish and start everything shortly after startup so the simulator
            // Widgets preview window (opened by the same system property) has content to render
            // without any manual clicks.
            UITimer.timer(2000, false, f, () -> {
                publishDeliveryTimeline();
                publishClockTimeline();
                startDeliveryActivity();
            });
        }
    }

    public void stop() {
        current = CN.getCurrentForm();
        if (current instanceof Dialog) {
            ((Dialog) current).dispose();
            current = CN.getCurrentForm();
        }
    }

    public void destroy() {
    }

    /**
     * Called periodically by the platform while the app is in the background (simulate with
     * "Pause App" in the simulator). Re-publishes the delivery timeline with fresh dates, then
     * signals completion -- the callback MUST be invoked or iOS stops granting fetch time.
     */
    @Override
    public void performBackgroundFetch(long deadline, Callback<Boolean> onComplete) {
        publishDeliveryTimeline();
        publishClockTimeline();
        onComplete.onSucess(Boolean.TRUE);
    }

    /**
     * Publishes a four-entry timeline covering the next four minutes: the OS flips entries on
     * schedule and interpolates each entry's state map into the layout's ${key} placeholders,
     * all without waking the app. The countdown ticks natively between refreshes.
     */
    private void publishDeliveryTimeline() {
        long now = System.currentTimeMillis();
        long eta = now + 4 * 60000L;
        WidgetTimeline timeline = new WidgetTimeline()
                .setContent(buildDeliveryLayout())
                .addEntry(new Date(now), deliveryState("Preparing your order", eta, 0.1f))
                .addEntry(new Date(now + 60000L), deliveryState("Out for delivery", eta, 0.4f))
                .addEntry(new Date(now + 3 * 60000L), deliveryState("Arriving now", eta, 0.9f))
                .addEntry(new Date(eta), deliveryState("Delivered", eta, 1f))
                .setReloadPolicy(WidgetTimeline.RELOAD_AT_END);
        Surfaces.publish(KIND_DELIVERY, timeline);
    }

    /**
     * Publishes the analog clock widget: a {@link SurfaceVector} face published once, plus 60
     * per-minute timeline entries carrying only the {@code hourAngle}/{@code minuteAngle} state
     * the rotation groups read. The OS flips entries on its own schedule, so the clock stays
     * correct for an hour with zero app wakeups; the background fetch hook re-publishes the next
     * hour of entries.
     */
    private void publishClockTimeline() {
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
        Surfaces.publish(KIND_CLOCK, timeline);
    }

    /**
     * The clock face: a filled dial with twelve tick marks, hour/minute hands driven by
     * state-keyed rotation groups and a center hub. Everything is resolution independent --
     * the 200x200 view box scales to whatever size the widget gets.
     */
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

    /**
     * The shared delivery layout: courier avatar, status line, OS-animated countdown to the ETA
     * and a progress bar. Text and values come from ${key} placeholders / state keys so updates
     * only ship a small state map. Tapping the surface delivers the "open_order" action.
     */
    private SurfaceNode buildDeliveryLayout() {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", "CN1-12345");
        return new SurfaceColumn().setSpacing(6).setPadding(12)
                .add(new SurfaceRow().setSpacing(10)
                        .add(new SurfaceImage(createCourierAvatar())
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

    /**
     * Starts the live activity for the running delivery. The descriptor carries the lock-screen
     * content plus the Dynamic Island regions; the returned handle is inert (isActive() false)
     * on platforms without live activity support, so no platform checks are needed here.
     */
    private void startDeliveryActivity() {
        if (activity != null && activity.isActive()) {
            Dialog.show("Live Activity", "The delivery activity is already running", "OK", null);
            return;
        }
        activityEta = System.currentTimeMillis() + 4 * 60000L;
        activityProgress = 0.25f;
        LiveActivityDescriptor descriptor = new LiveActivityDescriptor("delivery")
                .setContent(buildDeliveryLayout())
                .setCompactLeading(new SurfaceImage(createCourierAvatar())
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
                deliveryState("Out for delivery", activityEta, activityProgress));
        updateActivityLabel(activity.isActive()
                ? "Live activity: running (progress 25%)"
                : "Live activity: not supported on this platform");
    }

    /** Pushes fresh state to the running activity: +25% progress and a status change. */
    private void advanceActivity() {
        if (activity == null || !activity.isActive()) {
            Dialog.show("Live Activity", "Start the live activity first", "OK", null);
            return;
        }
        activityProgress = Math.min(1f, activityProgress + 0.25f);
        String status = activityProgress >= 1f ? "Arriving now" : "Out for delivery";
        activity.update(deliveryState(status, activityEta, activityProgress));
        updateActivityLabel("Live activity: running (progress "
                + (int) (activityProgress * 100) + "%)");
    }

    /** Ends the activity with a final "Delivered" state the platform lingers on briefly. */
    private void endActivity() {
        if (activity == null || !activity.isActive()) {
            Dialog.show("Live Activity", "No live activity is running", "OK", null);
            return;
        }
        activity.end(deliveryState("Delivered", System.currentTimeMillis(), 1f));
        updateActivityLabel("Live activity: ended");
    }

    private void updateActivityLabel(String text) {
        activityLabel.setText(text);
        if (CN.getCurrentForm() != null) {
            CN.getCurrentForm().revalidate();
        }
    }

    /** The "order" form an "open_order" surface tap navigates to, showing the action payload. */
    private void showOrderForm(SurfaceActionEvent evt) {
        Form order = new Form("Order", BoxLayout.y());
        order.add(new Label("Order: " + evt.getParams().get("orderId")));
        order.add(new Label("Opened from surface: " + evt.getSource()));
        order.add(new Label("Cold start: " + evt.isColdStart()));
        Button back = new Button("Back");
        back.addActionListener(e -> {
            if (mainForm != null) {
                mainForm.show();
            }
        });
        order.add(back);
        order.show();
    }

    /**
     * A small generated mutable image used as the courier avatar. Generated images exercise the
     * serializer's PNG encoding path; a real app would typically ship a bundled EncodedImage.
     */
    private Image createCourierAvatar() {
        Image avatar = Image.createImage(40, 40, ACCENT_COLOR);
        Graphics g = avatar.getGraphics();
        g.setColor(0xffffff);
        g.fillArc(10, 6, 20, 20, 0, 360);
        g.fillArc(6, 28, 28, 18, 0, 360);
        return avatar;
    }

    private Map<String, Object> deliveryState(String status, long eta, float progress) {
        Map<String, Object> state = new HashMap<>();
        state.put("status", status);
        state.put("eta", eta);
        state.put("progress", progress);
        return state;
    }
}
