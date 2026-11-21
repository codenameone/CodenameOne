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

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.videobuilder.DemoContext;
import com.codename1.videobuilder.DemoScene;
import com.codename1.vr.Media360View;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

/** A compiled Media360View demo; scripted actions change the component, not its screenshot. */
public final class Media360DemoScene implements DemoScene {
    private Media360View view;
    private PanoramaSurface surface;

    @Override
    public Component create(DemoContext context) {
        view = new Media360View();
        try (InputStream input = Files.newInputStream(context.resolveAsset("vr-360-panorama.png"))) {
            Image panorama = Image.createImage(input);
            view.setImage(panorama);
            surface = new PanoramaSurface(panorama);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load panorama", ex);
        }
        return surface;
    }

    @Override
    public void onAction(String name, Map<String, Object> arguments) {
        if ("lookRight".equals(name)) {
            float yaw = number(arguments.get("yaw"), 42f);
            view.setYaw(yaw);
            surface.yaw = yaw;
        } else if ("lookUp".equals(name)) {
            float pitch = number(arguments.get("pitch"), 18f);
            view.setPitch(pitch);
            surface.pitch = pitch;
        } else if ("stereo".equals(name)) {
            view.setStereo(true);
            surface.stereo = true;
        } else if ("recenter".equals(name)) {
            view.reset();
            surface.yaw = 0f;
            surface.pitch = 0f;
        } else {
            throw new IllegalArgumentException("Unknown Media360 demo action: " + name);
        }
        surface.repaint();
    }

    private static float number(Object value, float fallback) {
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }

    @Override
    public void reset() {
        if (view != null) {
            view.setStereo(false);
            view.reset();
        }
        if (surface != null) {
            surface.yaw = 0f;
            surface.pitch = 0f;
            surface.stereo = false;
            surface.repaint();
        }
    }

    @Override
    public void dispose() {
        view = null;
        surface = null;
    }

    /** Offscreen-safe instrumentation of the state applied to the real Media360View. */
    private static final class PanoramaSurface extends Component {
        private final Image panorama;
        private float yaw;
        private float pitch;
        private boolean stereo;

        PanoramaSurface(Image panorama) {
            this.panorama = panorama;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int header = Math.max(36, h / 10);
            int footer = Math.max(32, h / 12);
            g.setColor(0x0b1220);
            g.fillRect(x, y, w, h);
            if (stereo) {
                int eyeWidth = (w - 6) / 2;
                drawEye(g, x, y + header, eyeWidth, h - header - footer, yaw - 3f, pitch);
                drawEye(g, x + eyeWidth + 6, y + header, eyeWidth,
                        h - header - footer, yaw + 3f, pitch);
                g.setColor(0xf3f7fb);
                g.fillRect(x + eyeWidth, y + header, 6, h - header - footer);
            } else {
                drawEye(g, x, y + header, w, h - header - footer, yaw, pitch);
            }

            Font old = g.getFont();
            Font label = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
            Font body = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setFont(label);
            g.setColor(0x50d8ff);
            g.drawString("YAW " + Math.round(yaw) + "°", x + 14, y + 10);
            g.setColor(0xffc857);
            g.drawString("PITCH " + Math.round(pitch) + "°", x + w * 36 / 100, y + 10);
            g.setColor(0xf3f7fb);
            g.drawString(stereo ? "STEREO ON" : "MONO", x + w * 72 / 100, y + 10);
            g.setFont(body);
            g.setColor(0xcbd5e1);
            g.drawString("RUNNING Media360View · GPU state", x + 14, y + h - footer + 6);
            g.setFont(old);
        }

        private void drawEye(Graphics g, int x, int y, int width, int height,
                             float eyeYaw, float eyePitch) {
            int drawWidth = width * 2;
            int drawHeight = height;
            int shiftX = Math.round((eyeYaw / 360f) * drawWidth);
            int shiftY = Math.round((eyePitch / 90f) * height / 4f);
            int baseX = x - shiftX;
            while (baseX > x) baseX -= drawWidth;
            while (baseX + drawWidth < x) baseX += drawWidth;
            g.drawImage(panorama, baseX, y + shiftY, drawWidth, drawHeight);
            g.drawImage(panorama, baseX + drawWidth, y + shiftY, drawWidth, drawHeight);
            g.setColor(0x50d8ff);
            g.drawArc(x + width / 2 - 18, y + height / 2 - 18, 36, 36, 0, 360);
            g.drawLine(x + width / 2 - 28, y + height / 2, x + width / 2 - 10, y + height / 2);
            g.drawLine(x + width / 2 + 10, y + height / 2, x + width / 2 + 28, y + height / 2);
        }
    }
}
