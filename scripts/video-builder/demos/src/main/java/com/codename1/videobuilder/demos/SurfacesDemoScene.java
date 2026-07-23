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
package com.codename1.videobuilder.demos;

import com.codename1.surfaces.LiveActivity;
import com.codename1.surfaces.LiveActivityDescriptor;
import com.codename1.surfaces.SurfaceActionEvent;
import com.codename1.surfaces.SurfaceColor;
import com.codename1.surfaces.SurfaceColumn;
import com.codename1.surfaces.SurfaceDynamicText;
import com.codename1.surfaces.SurfaceFontWeight;
import com.codename1.surfaces.SurfaceProgress;
import com.codename1.surfaces.SurfaceText;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.WidgetKind;
import com.codename1.surfaces.WidgetSize;
import com.codename1.surfaces.WidgetTimeline;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.videobuilder.DemoContext;
import com.codename1.videobuilder.DemoScene;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** Compiled external-surfaces demo whose actions use the shipped public API. */
public final class SurfacesDemoScene implements DemoScene {
    private static final String KIND = "delivery_status";
    private static final String[] STATUSES = {
        "Preparing your order", "Out for delivery", "Arriving now", "Delivered"
    };
    private static final int[] PROGRESS = {10, 40, 90, 100};
    private static final String[] TIMES = {"3:42", "2:31", "0:48", "DONE"};

    private Container root;
    private DeliverySurface surface;
    private LiveActivity activity;
    private int timelineIndex;
    private int activityProgress;

    @Override
    public Component create(DemoContext context) {
        root = new Container(new BorderLayout());
        surface = new DeliverySurface();
        root.add(BorderLayout.CENTER, surface);
        Surfaces.registerWidgetKind(new WidgetKind(KIND)
                .setDisplayName("Delivery")
                .setDescription("Track your order")
                .addSupportedSize(WidgetSize.SMALL)
                .addSupportedSize(WidgetSize.MEDIUM));
        installActionHandler();
        return root;
    }

    private void installActionHandler() {
        Surfaces.setActionHandler(this::showAction);
    }

    @Override
    public void onAction(String name, Map<String, Object> arguments) {
        if ("publish".equals(name)) {
            publishTimeline();
        } else if ("nextTimeline".equals(name)) {
            timelineIndex = Math.min(STATUSES.length - 1, timelineIndex + 1);
            surface.timelineIndex = timelineIndex;
            surface.published = true;
            surface.repaint();
        } else if ("resize".equals(name)) {
            surface.medium = !surface.medium;
            surface.repaint();
        } else if ("startActivity".equals(name)) {
            startActivity();
        } else if ("advanceActivity".equals(name)) {
            advanceActivity();
        } else if ("tapSurface".equals(name)) {
            queueColdStartAction();
        } else if ("reset".equals(name)) {
            reset();
        } else {
            throw new IllegalArgumentException("Unknown surfaces demo action: " + name);
        }
    }

    private void publishTimeline() {
        long now = System.currentTimeMillis();
        long eta = now + 4 * 60000L;
        WidgetTimeline timeline = new WidgetTimeline()
                .setContent(deliveryLayout())
                .addEntry(new Date(now), state(STATUSES[0], eta, 0.1f))
                .addEntry(new Date(now + 60000L), state(STATUSES[1], eta, 0.4f))
                .addEntry(new Date(now + 3 * 60000L), state(STATUSES[2], eta, 0.9f))
                .addEntry(new Date(eta), state(STATUSES[3], eta, 1f))
                .setReloadPolicy(WidgetTimeline.RELOAD_AT_END);
        Surfaces.publish(KIND, timeline);
        timelineIndex = 0;
        surface.timelineIndex = 0;
        surface.published = true;
        surface.actionMessage = "";
        surface.repaint();
    }

    private void startActivity() {
        long eta = System.currentTimeMillis() + 4 * 60000L;
        LiveActivityDescriptor descriptor = new LiveActivityDescriptor("delivery")
                .setContent(deliveryLayout())
                .setCompactLeading(new SurfaceText("PKG").setFontWeight(SurfaceFontWeight.BOLD))
                .setCompactTrailing(new SurfaceDynamicText(
                        SurfaceDynamicText.STYLE_TIMER_DOWN, "eta"))
                .setExpandedCenter(new SurfaceText("${status}"))
                .setExpandedBottom(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                        .setValueState("progress"))
                .setTint(SurfaceColor.rgb(0xffff8a34));
        activityProgress = 25;
        activity = LiveActivity.start(
                descriptor, state("Out for delivery", eta, activityProgress / 100f));
        surface.activityStarted = activity.isActive();
        surface.activityProgress = activityProgress;
        surface.repaint();
    }

    private void advanceActivity() {
        if (activity == null || !activity.isActive()) {
            startActivity();
            return;
        }
        activityProgress = Math.min(100, activityProgress + 25);
        String status = activityProgress >= 100 ? "Arriving now" : "Out for delivery";
        activity.update(state(status, System.currentTimeMillis() + 90000L,
                activityProgress / 100f));
        surface.activityStarted = true;
        surface.activityProgress = activityProgress;
        surface.repaint();
    }

    private void queueColdStartAction() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("orderId", "CN1-12345");
        Surfaces.setActionHandler(null);
        Surfaces.dispatchAction(KIND, "open_order", params);
        installActionHandler();
        // The public API drains the queued event asynchronously on the EDT. Mirror the
        // expected result immediately so an offscreen frame capture does not race that drain.
        surface.actionMessage = "ORDER CN1-12345 OPENED AFTER COLD START";
        surface.repaint();
    }

    private void showAction(SurfaceActionEvent event) {
        if (surface == null) {
            return;
        }
        surface.actionMessage = "ORDER " + event.getParams().get("orderId")
                + (event.isColdStart() ? " OPENED AFTER COLD START" : " OPENED");
        surface.repaint();
    }

    private SurfaceColumn deliveryLayout() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("orderId", "CN1-12345");
        return new SurfaceColumn().setPadding(12).setSpacing(7)
                .add(new SurfaceText("${status}")
                        .setFontSize(15).setFontWeight(SurfaceFontWeight.SEMIBOLD))
                .add(new SurfaceDynamicText(SurfaceDynamicText.STYLE_TIMER_DOWN, "eta")
                        .setFontSize(24).setColor(SurfaceColor.ACCENT))
                .add(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                        .setValueState("progress"))
                .setAction("open_order", params);
    }

    private Map<String, Object> state(String status, long eta, float progress) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("status", status);
        values.put("eta", Long.valueOf(eta));
        values.put("progress", Float.valueOf(progress));
        return values;
    }

    @Override
    public void reset() {
        timelineIndex = 0;
        activityProgress = 0;
        if (surface != null) {
            surface.published = false;
            surface.medium = true;
            surface.timelineIndex = 0;
            surface.activityStarted = false;
            surface.activityProgress = 0;
            surface.actionMessage = "";
            surface.repaint();
        }
    }

    @Override
    public void dispose() {
        if (activity != null && activity.isActive()) {
            activity.end(state("Delivered", System.currentTimeMillis(), 1f));
        }
        Surfaces.setActionHandler(null);
        activity = null;
        surface = null;
        root = null;
    }

    private static final class DeliverySurface extends Component {
        private boolean published;
        private boolean medium = true;
        private int timelineIndex;
        private boolean activityStarted;
        private int activityProgress;
        private String actionMessage = "";

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            g.setColor(0x081321);
            g.fillRect(x, y, w, h);

            Font small = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            Font mediumFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            Font large = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);

            g.setFont(small);
            g.setColor(0x77dfff);
            g.drawString("SYSTEM SURFACE", x + w * 7 / 100, y + h * 7 / 100);
            g.setColor(0x60758a);
            g.drawString("APP PROCESS ASLEEP", x + w * 58 / 100, y + h * 7 / 100);
            g.setColor(0x25384b);
            g.drawLine(x + w * 7 / 100, y + h * 12 / 100,
                    x + w * 93 / 100, y + h * 12 / 100);

            int cardX = x + w * (medium ? 8 : 19) / 100;
            int cardY = y + h * 18 / 100;
            int cardW = w * (medium ? 84 : 62) / 100;
            int cardH = h * 38 / 100;
            g.setColor(0x142438);
            g.fillRoundRect(cardX, cardY, cardW, cardH, 34, 34);
            g.setColor(0x425b72);
            g.drawRoundRect(cardX, cardY, cardW, cardH, 34, 34);

            int icon = Math.max(34, Math.min(cardH * 38 / 100, cardW / 5));
            int iconX = cardX + cardW * 7 / 100;
            int iconY = cardY + cardH * 21 / 100;
            g.setColor(0xff8a34);
            g.fillRoundRect(iconX, iconY, icon, icon, 18, 18);
            g.setColor(0x081321);
            g.fillRect(iconX + icon * 23 / 100, iconY + icon * 33 / 100,
                    icon * 54 / 100, icon * 40 / 100);
            g.setColor(0xffc28f);
            g.drawLine(iconX + icon / 2, iconY + icon * 18 / 100,
                    iconX + icon / 2, iconY + icon * 82 / 100);

            int textX = iconX + icon + cardW * 5 / 100;
            g.setFont(mediumFont);
            g.setColor(0xf5f8fb);
            g.drawString(published ? STATUSES[timelineIndex] : "Timeline not published",
                    textX, cardY + cardH * 24 / 100);
            g.setFont(large);
            g.setColor(0x50d8ff);
            g.drawString(published ? TIMES[timelineIndex] : "--:--",
                    textX, cardY + cardH * 48 / 100);

            int progressX = textX;
            int progressY = cardY + cardH * 70 / 100;
            int progressW = cardX + cardW - cardW * 7 / 100 - progressX;
            g.setColor(0x34475c);
            g.fillRoundRect(progressX, progressY, progressW, 10, 10, 10);
            g.setColor(0xff8a34);
            int progress = published ? PROGRESS[timelineIndex] : 0;
            g.fillRoundRect(progressX, progressY, progressW * progress / 100, 10, 10, 10);

            g.setFont(small);
            g.setColor(0x90a5b8);
            g.drawString(published ? "TIMELINE ENTRY " + (timelineIndex + 1) + " OF 4"
                    : "PUBLISH A SERIALIZABLE TIMELINE",
                    cardX, cardY + cardH + h * 5 / 100);
            g.drawString(medium ? "MEDIUM" : "SMALL",
                    cardX + cardW - small.stringWidth(medium ? "MEDIUM" : "SMALL"),
                    cardY + cardH + h * 5 / 100);

            int islandX = x + w * 20 / 100;
            int islandY = y + h * 68 / 100;
            int islandW = w * 60 / 100;
            int islandH = h * 12 / 100;
            g.setColor(0x000000);
            g.fillRoundRect(islandX, islandY, islandW, islandH, islandH, islandH);
            g.setFont(mediumFont);
            g.setColor(activityStarted ? 0xff8a34 : 0x5b6875);
            g.drawString(activityStarted ? "LIVE" : "INACTIVE",
                    islandX + islandW * 8 / 100, islandY + islandH * 34 / 100);
            g.setColor(0xf2f4f7);
            String activityText = activityStarted
                    ? "Delivery " + activityProgress + "%" : "Live Activity";
            g.drawString(activityText, islandX + islandW * 34 / 100,
                    islandY + islandH * 34 / 100);

            if (!actionMessage.isEmpty()) {
                g.setColor(0x143d2e);
                g.fillRoundRect(x + w * 10 / 100, y + h * 86 / 100,
                        w * 80 / 100, h * 8 / 100, 20, 20);
                g.setColor(0x7ce6ad);
                g.setFont(small);
                int messageX = x + (w - small.stringWidth(actionMessage)) / 2;
                g.drawString(actionMessage, messageX, y + h * 88 / 100);
            }
        }
    }
}
