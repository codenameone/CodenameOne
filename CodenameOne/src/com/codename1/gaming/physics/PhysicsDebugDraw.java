/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gaming.physics;

import com.codename1.gaming.physics.box2d.callbacks.DebugDraw;
import com.codename1.gaming.physics.box2d.common.Color3f;
import com.codename1.gaming.physics.box2d.common.OBBViewportTransform;
import com.codename1.gaming.physics.box2d.common.Transform;
import com.codename1.gaming.physics.box2d.common.Vec2;
import com.codename1.ui.Graphics;

/// Renders a `PhysicsWorld`'s collision shapes, joints and bounding boxes onto a
/// Codename One `com.codename1.ui.Graphics`, for debugging. It converts Box2D's
/// meters (y up) to the world's pixels (y down) using the same scale and flip the
/// rest of the wrapper uses, so the debug overlay lines up exactly with the sprites
/// driven by the bodies.
///
/// You normally do not create this yourself -- call
/// `PhysicsWorld#debugDraw(com.codename1.ui.Graphics)` from a component's `paint`,
/// onto an off-screen `com.codename1.ui.Image`, or anywhere you have a `Graphics`.
public class PhysicsDebugDraw extends DebugDraw {
    private final PhysicsWorld world;
    private Graphics g;
    private int fillAlpha = 90;

    /// Creates a debug renderer for the given world.
    public PhysicsDebugDraw(PhysicsWorld world) {
        super(new OBBViewportTransform());
        this.world = world;
    }

    /// Sets the alpha (0..255) used to fill solid shapes; outlines are always opaque.
    public void setFillAlpha(int alpha) {
        this.fillAlpha = alpha;
    }

    /// The graphics the next `drawDebugData` pass renders to (set by `PhysicsWorld`).
    void setGraphics(Graphics graphics) {
        this.g = graphics;
    }

    private int px(float meters) {
        return (int) world.toPixels(meters);
    }

    private int py(float meters) {
        return (int) -world.toPixels(meters);
    }

    private static int rgb(Color3f c) {
        int r = clamp(c.x);
        int gr = clamp(c.y);
        int b = clamp(c.z);
        return (r << 16) | (gr << 8) | b;
    }

    private static int clamp(float channel) {
        int v = (int) (channel * 255);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    @Override
    public void drawPolygon(Vec2[] vertices, int vertexCount, Color3f color) {
        int[] xs = new int[vertexCount];
        int[] ys = new int[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            xs[i] = px(vertices[i].x);
            ys[i] = py(vertices[i].y);
        }
        g.setColor(rgb(color));
        g.drawPolygon(xs, ys, vertexCount);
    }

    @Override
    public void drawSolidPolygon(Vec2[] vertices, int vertexCount, Color3f color) {
        int[] xs = new int[vertexCount];
        int[] ys = new int[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            xs[i] = px(vertices[i].x);
            ys[i] = py(vertices[i].y);
        }
        int c = rgb(color);
        int oldAlpha = g.getAlpha();
        g.setColor(c);
        g.setAlpha(fillAlpha);
        g.fillPolygon(xs, ys, vertexCount);
        g.setAlpha(oldAlpha);
        g.drawPolygon(xs, ys, vertexCount);
    }

    @Override
    public void drawCircle(Vec2 center, float radius, Color3f color) {
        int r = px(radius);
        int cx = px(center.x);
        int cy = py(center.y);
        g.setColor(rgb(color));
        g.drawArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
    }

    @Override
    public void drawSolidCircle(Vec2 center, float radius, Vec2 axis, Color3f color) {
        int r = px(radius);
        int cx = px(center.x);
        int cy = py(center.y);
        int c = rgb(color);
        int oldAlpha = g.getAlpha();
        g.setColor(c);
        g.setAlpha(fillAlpha);
        g.fillArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
        g.setAlpha(oldAlpha);
        g.drawArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
        if (axis != null) {
            // a spoke showing the body's orientation
            g.drawLine(cx, cy, px(center.x + radius * axis.x), py(center.y + radius * axis.y));
        }
    }

    @Override
    public void drawSegment(Vec2 p1, Vec2 p2, Color3f color) {
        g.setColor(rgb(color));
        g.drawLine(px(p1.x), py(p1.y), px(p2.x), py(p2.y));
    }

    @Override
    public void drawTransform(Transform xf) {
        float len = world.toMeters(12);   // ~12px axes
        int ox = px(xf.p.x);
        int oy = py(xf.p.y);
        // x axis (red), y axis (green) of the body frame
        g.setColor(0xff0000);
        g.drawLine(ox, oy, px(xf.p.x + xf.q.getCos() * len), py(xf.p.y + xf.q.getSin() * len));
        g.setColor(0x00ff00);
        g.drawLine(ox, oy, px(xf.p.x - xf.q.getSin() * len), py(xf.p.y + xf.q.getCos() * len));
    }

    @Override
    public void drawPoint(Vec2 argPoint, float argRadiusOnScreen, Color3f color) {
        int r = Math.max(1, (int) argRadiusOnScreen);
        int cx = px(argPoint.x);
        int cy = py(argPoint.y);
        g.setColor(rgb(color));
        g.fillArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
    }

    @Override
    public void drawString(float x, float y, String s, Color3f color) {
        if (s == null) {
            return;
        }
        g.setColor(rgb(color));
        g.drawString(s, (int) x, (int) y);
    }
}
