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

package com.codename1.impl.html5.graphics;

import com.codename1.html5.js.canvas.CanvasGradient;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.impl.html5.HTML5Graphics;

public class FillRectRadialGradient implements ExecutableOp {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int startColor;
    private final int endColor;
    private final float relativeX;
    private final float relativeY;
    private final float relativeSize;
    private final int alpha;

    public FillRectRadialGradient(int x, int y, int width, int height, int startColor, int endColor,
            float relativeX, float relativeY, float relativeSize, int alpha) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.startColor = startColor;
        this.endColor = endColor;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.relativeSize = relativeSize;
        this.alpha = alpha;
    }

    @Override
    public void execute(CanvasRenderingContext2D context) {
        execute(context, x, y, width, height, startColor, endColor, relativeX, relativeY, relativeSize, alpha);
    }

    public static void execute(CanvasRenderingContext2D context, int x, int y, int width, int height,
            int startColor, int endColor, float relativeX, float relativeY, float relativeSize, int alpha) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int size = (int)(Math.min(width, height) * relativeSize);
        if (size <= 0) {
            new FillRect(x, y, width, height, endColor, alpha).execute(context);
            return;
        }

        int x2 = (int)(width * 0.5f - (size * relativeX));
        int y2 = (int)(height * 0.5f - (size * relativeY));
        double centerX = x + x2 + size / 2.0;
        double centerY = y + y2 + size / 2.0;
        double radius = size / 2.0;

        int startAlpha = (startColor >> 24) & 0xFF;
        int endAlpha = (endColor >> 24) & 0xFF;
        String startColorStr = startAlpha > 0 ? HTML5Graphics.colorWithAlpha(startColor) : HTML5Graphics.color(startColor);
        String endColorStr = endAlpha > 0 ? HTML5Graphics.colorWithAlpha(endColor) : HTML5Graphics.color(endColor);

        CanvasGradient gradient = context.createRadialGradient(centerX, centerY, 0, centerX, centerY, radius);
        gradient.addColorStop(0, startColorStr);
        gradient.addColorStop(1, endColorStr);

        context.save();
        context.setFillStyle(gradient);
        context.setGlobalAlpha(((double)alpha) / 255.0);
        context.beginPath();
        context.rect(x, y, width, height);
        context.fill();
        context.restore();
    }

    @Override
    public String getDescription() {
        return "FillRectRadialGradient";
    }
}
