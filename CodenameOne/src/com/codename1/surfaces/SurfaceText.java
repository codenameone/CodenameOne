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
package com.codename1.surfaces;

import java.util.Map;

/// A static text node. The text may embed `${key}` placeholders that the platform renderer
/// substitutes from the state map of the current timeline entry or live-activity update, so
/// frequently changing values do not require re-publishing the whole descriptor:
///
/// ```java
/// new SurfaceText("${status}").setFontSize(13).setColor(SurfaceColor.SECONDARY_LABEL)
/// ```
///
/// For values the OS should animate on its own clock (countdowns, elapsed time) use
/// `SurfaceDynamicText` instead.
public class SurfaceText extends SurfaceNode {
    private String text;
    private int fontSize;
    private SurfaceFontWeight fontWeight;
    private SurfaceColor color;
    private int maxLines;

    /// Creates a text node.
    ///
    /// #### Parameters
    ///
    /// - `text`: literal text, optionally embedding `${key}` state placeholders
    public SurfaceText(String text) {
        this.text = text;
    }

    /// Sets the font size.
    ///
    /// #### Parameters
    ///
    /// - `fontSize`: the size in dips
    ///
    /// #### Returns
    ///
    /// this text node, for chaining
    public SurfaceText setFontSize(int fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    /// Sets the font weight.
    ///
    /// #### Parameters
    ///
    /// - `fontWeight`: the weight
    ///
    /// #### Returns
    ///
    /// this text node, for chaining
    public SurfaceText setFontWeight(SurfaceFontWeight fontWeight) {
        this.fontWeight = fontWeight;
        return this;
    }

    /// Sets the text color.
    ///
    /// #### Parameters
    ///
    /// - `color`: the color
    ///
    /// #### Returns
    ///
    /// this text node, for chaining
    public SurfaceText setColor(SurfaceColor color) {
        this.color = color;
        return this;
    }

    /// Limits the number of rendered lines.
    ///
    /// #### Parameters
    ///
    /// - `maxLines`: maximum line count, 0 for unlimited
    ///
    /// #### Returns
    ///
    /// this text node, for chaining
    public SurfaceText setMaxLines(int maxLines) {
        this.maxLines = maxLines;
        return this;
    }

    /// Returns the text, potentially containing `${key}` placeholders.
    public String getText() {
        return text;
    }

    /// Returns the font size in dips, 0 for the platform default.
    public int getFontSize() {
        return fontSize;
    }

    /// Returns the font weight, or null for the platform default.
    public SurfaceFontWeight getFontWeight() {
        return fontWeight;
    }

    /// Returns the text color, or null for the platform default.
    public SurfaceColor getColor() {
        return color;
    }

    /// Returns the maximum line count, 0 for unlimited.
    public int getMaxLines() {
        return maxLines;
    }

    @Override
    public SurfaceText setPadding(int all) {
        super.setPadding(all);
        return this;
    }

    @Override
    public SurfaceText setPadding(int top, int right, int bottom, int left) {
        super.setPadding(top, right, bottom, left);
        return this;
    }

    @Override
    public SurfaceText setBackground(SurfaceColor background) {
        super.setBackground(background);
        return this;
    }

    @Override
    public SurfaceText setCornerRadius(int radius) {
        super.setCornerRadius(radius);
        return this;
    }

    @Override
    public SurfaceText setAlignment(SurfaceAlignment alignment) {
        super.setAlignment(alignment);
        return this;
    }

    @Override
    public SurfaceText setWeight(int weight) {
        super.setWeight(weight);
        return this;
    }

    @Override
    public SurfaceText setSize(int widthDips, int heightDips) {
        super.setSize(widthDips, heightDips);
        return this;
    }

    @Override
    public SurfaceText setAction(String actionId) {
        super.setAction(actionId);
        return this;
    }

    @Override
    public SurfaceText setAction(String actionId, Map<String, Object> params) {
        super.setAction(actionId, params);
        return this;
    }

    @Override
    String getType() {
        return "text";
    }

    @Override
    void serializeContent(Map<String, Object> out, Map<String, byte[]> images, int depth) {
        out.put("text", text == null ? "" : text);
        if (fontSize != 0) {
            out.put("size", Integer.valueOf(fontSize));
        }
        if (fontWeight != null) {
            out.put("fw", fontWeight.getJsonName());
        }
        if (color != null) {
            out.put("color", SurfaceSerializer.colorMap(color));
        }
        if (maxLines != 0) {
            out.put("maxLines", Integer.valueOf(maxLines));
        }
    }
}
