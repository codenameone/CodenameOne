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

import com.codename1.ar.AR;
import com.codename1.ar.ARAnchor;
import com.codename1.ar.ARModel;
import com.codename1.ar.ARNode;
import com.codename1.ar.ARPlaneDetection;
import com.codename1.ar.ARPose;
import com.codename1.ar.ARSession;
import com.codename1.ar.ARSessionOptions;
import com.codename1.ar.ARTrackingFailureReason;
import com.codename1.ar.ARTrackingState;
import com.codename1.gpu.Primitives;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.videobuilder.DemoContext;
import com.codename1.videobuilder.DemoScene;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/** A compiled AR session whose scripted actions mutate the real JavaSE backend. */
public final class ARDemoScene implements DemoScene {
    private Container root;
    private RoomSurface room;
    private ARSession session;
    private ARAnchor anchor;
    private ARNode model;

    @Override
    public Component create(DemoContext context) {
        root = new Container(new BorderLayout());
        room = new RoomSurface();
        session = AR.open(new ARSessionOptions().planeDetection(ARPlaneDetection.HORIZONTAL));
        session.addTrackingListener((current, state, reason) -> setTracking(state, reason));
        session.addPlaneListener(event -> {
            room.planeCount = currentPlaneCount();
            room.repaint();
        });
        // Create the real AR peer so anchors and node changes exercise the same backend. Native
        // peers do not paint into the offscreen CI framebuffer, so RoomSurface instruments that
        // session state with deterministic Codename One drawing.
        session.createView();
        root.add(BorderLayout.CENTER, room);
        return root;
    }

    private int currentPlaneCount() {
        return session == null ? 0 : session.getPlanes().length;
    }

    private void setTracking(ARTrackingState state, ARTrackingFailureReason reason) {
        room.trackingState = state;
        room.failureReason = reason;
        room.repaint();
    }

    @Override
    public void onAction(String name, Map<String, Object> arguments) {
        if ("placeModel".equals(name)) {
            placeModel();
        } else if ("scaleModel".equals(name)) {
            requireModel().setLocalScale(number(arguments.get("scale"), 1.7f));
            room.modelScale = number(arguments.get("scale"), 1.7f);
            room.modelState = "SCALED";
            room.repaint();
        } else if ("moveModel".equals(name)) {
            float x = number(arguments.get("x"), 0.22f);
            requireModel().setLocalPosition(x, 0f, 0f);
            room.modelX = 0.5f + x;
            room.modelState = "MOVED";
            room.repaint();
        } else if ("trackingLimited".equals(name)) {
            room.trackingState = ARTrackingState.LIMITED;
            room.failureReason = ARTrackingFailureReason.EXCESSIVE_MOTION;
            room.repaint();
            setSimulatorTracking(ARTrackingState.LIMITED, ARTrackingFailureReason.EXCESSIVE_MOTION);
        } else if ("trackingNormal".equals(name)) {
            room.trackingState = ARTrackingState.TRACKING;
            room.failureReason = ARTrackingFailureReason.NONE;
            room.repaint();
            setSimulatorTracking(ARTrackingState.TRACKING, ARTrackingFailureReason.NONE);
        } else {
            throw new IllegalArgumentException("Unknown AR demo action: " + name);
        }
    }

    private void placeModel() {
        if (anchor == null) {
            detectSimulatorFloor();
            room.planeCount = Math.max(1, currentPlaneCount());
            anchor = session.createAnchor(new ARPose(0f, -0.35f, -1.45f, 0f, 0f, 0f, 1f));
            model = new ARNode(ARModel.fromMesh(Primitives.sphere(0.16f, 18, 24, false), 0xff50d8ff));
            anchor.setNode(model);
        }
        room.modelPlaced = true;
        room.modelState = "ANCHORED";
        room.repaint();
    }

    private ARNode requireModel() {
        if (model == null) {
            placeModel();
        }
        return model;
    }

    private static float number(Object value, float fallback) {
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }

    /** The simulator UI exposes these package-private hooks; reflection keeps them video-only. */
    private static void setSimulatorTracking(ARTrackingState state, ARTrackingFailureReason reason) {
        try {
            Class<?> type = Class.forName("com.codename1.impl.javase.JavaSEARImpl");
            Field active = type.getDeclaredField("activeInstance");
            active.setAccessible(true);
            Object backend = active.get(null);
            Method method = type.getDeclaredMethod("simSetTrackingState",
                    ARTrackingState.class, ARTrackingFailureReason.class);
            method.setAccessible(true);
            method.invoke(backend, state, reason);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot drive JavaSE AR simulation state", ex);
        }
    }

    private static void detectSimulatorFloor() {
        try {
            Class<?> type = Class.forName("com.codename1.impl.javase.JavaSEARImpl");
            Field active = type.getDeclaredField("activeInstance");
            active.setAccessible(true);
            Object backend = active.get(null);
            Method method = type.getDeclaredMethod("detectFloor");
            method.setAccessible(true);
            method.invoke(backend);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot detect a JavaSE simulation plane", ex);
        }
    }

    @Override
    public void reset() {
        if (model != null) {
            model.setLocalPosition(0f, 0f, 0f).setLocalScale(1f);
        }
        if (room != null) {
            room.modelX = 0.5f;
            room.modelScale = 1f;
            room.modelPlaced = model != null;
            room.modelState = model == null ? "NOT PLACED" : "ANCHORED";
            room.repaint();
        }
    }

    @Override
    public void dispose() {
        if (session != null) {
            session.close();
        }
        session = null;
        anchor = null;
        model = null;
        root = null;
        room = null;
    }

    /** Paints a deterministic view of the live session for offscreen video rendering. */
    private static final class RoomSurface extends Component {
        private ARTrackingState trackingState = ARTrackingState.NOT_TRACKING;
        private ARTrackingFailureReason failureReason = ARTrackingFailureReason.INITIALIZING;
        private int planeCount;
        private boolean modelPlaced;
        private float modelX = 0.5f;
        private float modelScale = 1f;
        private String modelState = "NOT PLACED";

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int horizon = y + h * 38 / 100;
            g.setColor(0x172033);
            g.fillRect(x, y, w, horizon - y);
            g.setColor(0x252f42);
            g.fillRect(x, horizon, w, y + h - horizon);

            g.setColor(0x3b4a63);
            for (int i = 0; i <= 7; i++) {
                int gx = x + i * w / 7;
                g.drawLine(x + w / 2, horizon, gx, y + h);
            }
            for (int i = 1; i <= 5; i++) {
                int gy = horizon + i * i * (h - (horizon - y)) / 30;
                g.drawLine(x, Math.min(y + h - 1, gy), x + w, Math.min(y + h - 1, gy));
            }

            int planeY = y + h * 69 / 100;
            int[] px = {x + w * 15 / 100, x + w * 85 / 100, x + w * 70 / 100, x + w * 30 / 100};
            int[] py = {planeY, planeY, y + h * 92 / 100, y + h * 92 / 100};
            g.setColor(0x194f61);
            g.fillPolygon(px, py, 4);
            g.setColor(0x50d8ff);
            g.drawPolygon(px, py, 4);

            int cx = x + w / 2;
            int cy = y + h * 57 / 100;
            g.setColor(0xf3f7fb);
            g.drawArc(cx - 20, cy - 20, 40, 40, 0, 360);
            g.drawLine(cx - 32, cy, cx - 12, cy);
            g.drawLine(cx + 12, cy, cx + 32, cy);
            g.drawLine(cx, cy - 32, cx, cy - 12);
            g.drawLine(cx, cy + 12, cx, cy + 32);

            if (modelPlaced) {
                int mx = x + Math.round(w * modelX);
                int floorY = y + h * 74 / 100;
                int radius = Math.max(18, Math.round(Math.min(w, h) * 0.075f * modelScale));
                g.setColor(0x101725);
                g.fillArc(mx - radius, floorY + radius / 2, radius * 2, radius / 2, 0, 360);
                g.setColor(0x1688a5);
                g.fillArc(mx - radius, floorY - radius * 2, radius * 2, radius * 2, 0, 360);
                g.setColor(0x50d8ff);
                g.fillArc(mx - radius * 3 / 5, floorY - radius * 9 / 5,
                        radius, radius, 0, 360);
                g.setColor(0xf3f7fb);
                g.drawLine(mx, floorY, mx, floorY + radius / 2);
            }

            Font old = g.getFont();
            Font label = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
            Font body = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setFont(label);
            g.setColor(0x0b1220);
            g.fillRect(x, y, w, Math.max(34, h / 9));
            g.setColor(trackingState == ARTrackingState.LIMITED ? 0xffc857 : 0x78e08f);
            String tracking = trackingState == ARTrackingState.NOT_TRACKING
                    ? "TRACK: OFF" : trackingState == ARTrackingState.TRACKING
                    ? "TRACK: OK" : "TRACK: LIMITED";
            g.drawString(tracking, x + 14, y + 10);
            g.setColor(0x50d8ff);
            g.drawString("PLANE " + planeCount, x + w * (w < 700 ? 55 : 38) / 100, y + 10);
            g.setColor(0xf3f7fb);
            if (w < 700) {
                g.drawString("MODEL: " + modelState, x + 14, y + 48);
            } else {
                g.drawString("MODEL " + modelState, x + w * 65 / 100, y + 10);
            }

            if (trackingState == ARTrackingState.LIMITED) {
                int bannerY = y + h * 18 / 100;
                g.setColor(0x3a2c16);
                g.fillRect(x + w / 8, bannerY, w * 3 / 4, Math.max(38, h / 8));
                g.setColor(0xffc857);
                g.drawRect(x + w / 8, bannerY, w * 3 / 4, Math.max(38, h / 8));
                g.drawString("LIMITED · EXCESSIVE MOTION", x + w / 8 + 12, bannerY + 12);
            }
            g.setFont(body);
            g.setColor(0xcbd5e1);
            g.drawString("RUNNING AR SESSION · JavaSE simulator", x + 14, y + h - 28);
            g.setFont(old);
        }
    }
}
