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
package com.codename1.impl.html5;

import com.codename1.impl.html5.graphics.ClipState;

public final class JavaScriptRenderState<F> {
    private final ClipState clipState = new ClipState();
    private F font;
    private int color;
    private int alpha = 0xff;
    // Canvas2D primitive paths are always AA; the toggle only meaningfully
    // affects ``drawImage`` smoothing. Track the requested state so
    // ``isAntiAliased{Text}`` returns what the caller last set even when
    // ``setAntiAliased(false)`` cannot disable path AA in canvas2d.
    private boolean antiAliased = true;
    private boolean antiAliasedText = true;
    private int renderingHints;

    public ClipState getClipState() {
        return clipState;
    }

    public F getFont() {
        return font;
    }

    public void setFont(F font) {
        this.font = font;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public boolean isAntiAliased() {
        return antiAliased;
    }

    public void setAntiAliased(boolean antiAliased) {
        this.antiAliased = antiAliased;
    }

    public boolean isAntiAliasedText() {
        return antiAliasedText;
    }

    public void setAntiAliasedText(boolean antiAliasedText) {
        this.antiAliasedText = antiAliasedText;
    }

    public int getRenderingHints() {
        return renderingHints;
    }

    public void setRenderingHints(int renderingHints) {
        this.renderingHints = renderingHints;
    }
}
