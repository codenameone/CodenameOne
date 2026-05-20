/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
