/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.ui;

import com.codename1.videobuilder.DemoContext;
import com.codename1.videobuilder.DemoScene;

import java.util.Map;

/** Compiled visualization driven by the production TabSelectionMorph model. */
public final class FidelityMorphDemoScene implements DemoScene {
    private MorphSurface surface;

    @Override
    public Component create(DemoContext context) {
        surface = new MorphSurface();
        return surface;
    }

    @Override
    public void onAction(String name, Map<String, Object> arguments) {
        if ("start".equals(name)) {
            surface.progress = 0.10f;
        } else if ("midpoint".equals(name)) {
            surface.progress = 0.50f;
        } else if ("settle".equals(name)) {
            surface.progress = 0.90f;
        } else if ("finish".equals(name)) {
            surface.progress = 1.00f;
        } else if ("toggleAppearance".equals(name)) {
            surface.dark = !surface.dark;
        } else if ("setProgress".equals(name)) {
            Object value = arguments.get("value");
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException("setProgress requires a numeric value");
            }
            surface.progress = Math.max(0f, Math.min(1f, ((Number) value).floatValue()));
        } else if ("reset".equals(name)) {
            reset();
            return;
        } else {
            throw new IllegalArgumentException("Unknown fidelity demo action: " + name);
        }
        surface.repaint();
    }

    @Override
    public void reset() {
        if (surface != null) {
            surface.progress = 0f;
            surface.dark = false;
            surface.repaint();
        }
    }

    @Override
    public void dispose() {
        surface = null;
    }

    private static final class MorphSurface extends Component {
        private float progress;
        private boolean dark;

        @Override
        public void paint(Graphics g) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            int background = dark ? 0x07111d : 0xeef3f8;
            int foreground = dark ? 0xf2f7fb : 0x152335;
            int muted = dark ? 0x8fa5ba : 0x5e7288;
            int barColor = dark ? 0x172536 : 0xd9e1e9;

            g.setColor(background);
            g.fillRect(x, y, width, height);

            int barLeft = x + width * 8 / 100;
            int barRight = x + width * 92 / 100;
            int barWidth = barRight - barLeft;
            int barTop = y + height * 47 / 100;
            int barHeight = height * 24 / 100;
            int cellWidth = barWidth / 3;
            int fromX = 0;
            int toX = cellWidth * 2;

            TabSelectionMorph.Tokens tokens = TabSelectionMorph.Tokens.preset("ios26");
            tokens.liftPx = Math.max(2, height / 80);
            tokens.downBiasPx = Math.max(1, height / 160);
            TabSelectionMorph model = TabSelectionMorph.compute(
                    progress, fromX, cellWidth, toX, cellWidth, barLeft,
                    barTop, barHeight, barLeft, barRight, tokens);

            g.setColor(barColor);
            g.fillRoundRect(barLeft, barTop, barWidth, barHeight, barHeight, barHeight);

            int oldAlpha = g.getAlpha();
            int pillAlpha = Math.max(35, Math.round(175 * (1f - model.flight)));
            g.setAlpha(pillAlpha);
            g.setColor(dark ? 0x8fa5ba : 0x7f91a5);
            g.fillRoundRect(model.capX, model.capY, model.capW, model.capH,
                    model.capH, model.capH);

            g.setAlpha(95 + Math.round(90 * model.flight));
            g.setColor(dark ? 0x5ad9ff : 0x4ecbe8);
            g.fillRoundRect(model.lensX, model.lensY, model.lensW, model.lensH,
                    model.lensH, model.lensH);
            g.setAlpha(oldAlpha);
            g.setColor(0xf8fbff);
            g.drawRoundRect(model.lensX, model.lensY, model.lensW, model.lensH,
                    model.lensH, model.lensH);

            Font oldFont = g.getFont();
            Font heading = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD,
                    Font.SIZE_MEDIUM);
            Font body = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,
                    Font.SIZE_SMALL);
            g.setFont(heading);
            g.setColor(foreground);
            g.drawString("SHIPPED TAB SELECTION MODEL", x + width * 8 / 100,
                    y + height * 10 / 100);
            g.setFont(body);
            g.setColor(muted);
            g.drawString(dark ? "DARK APPEARANCE" : "LIGHT APPEARANCE",
                    x + width * 8 / 100, y + height * 18 / 100);

            String[] labels = {"HOME", "SEARCH", "PROFILE"};
            for (int i = 0; i < labels.length; i++) {
                int textWidth = body.stringWidth(labels[i]);
                int textX = barLeft + i * cellWidth + (cellWidth - textWidth) / 2;
                g.setColor(i == 2 && progress > 0.74f ? foreground : muted);
                g.drawString(labels[i], textX, barTop + barHeight / 2 - body.getHeight() / 2);
            }

            g.setColor(foreground);
            g.drawString("progress " + Math.round(progress * 100f) + "%",
                    x + width * 8 / 100, y + height * 82 / 100);
            g.setColor(muted);
            g.drawString("flight " + decimal(model.flight)
                            + "   magnify " + decimal(model.magnify)
                            + "x   aberration " + decimal(model.aberration),
                    x + width * 8 / 100, y + height * 89 / 100);
            g.setFont(oldFont);
        }

        private static String decimal(float value) {
            int hundredths = Math.round(value * 100f);
            return (hundredths / 100) + "." + (hundredths % 100 < 10 ? "0" : "")
                    + (hundredths % 100);
        }
    }
}
