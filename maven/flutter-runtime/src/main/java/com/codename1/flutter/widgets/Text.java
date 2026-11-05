/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextOverflow;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;

/**
 * A run of styled text, backed by a CN1 Label (UIID "FlutterText").
 */
public class Text extends Widget {

    private final String data;
    private TextStyle style;
    private TextAlign textAlign;
    private String semanticsLabel;
    private TextOverflow overflow;
    private Long maxLines;

    public Text(String data) {
        this.data = data;
    }

    private TextSpan textSpan;

    /**
     * Flutter's {@code Text.rich}: text drawn from a span tree. It renders as a
     * {@link RichText} whose root span carries this widget's {@code style}, so the
     * span's own styles layer over it exactly as they do under a Text.
     */
    public static Text rich(TextSpan textSpan, com.codename1.flutter.Key key, TextStyle style,
                            TextAlign textAlign) {
        return rich(textSpan, key, style, textAlign, null);
    }

    /** {@code Text.rich} with a {@code textScaler} (null follows MediaQuery's). */
    public static Text rich(TextSpan textSpan, com.codename1.flutter.Key key, TextStyle style,
                            TextAlign textAlign, Object textScaler) {
        Text t = new Text(textSpan == null ? "" : textSpan.toPlainText());
        t.key(key);
        t.textSpan = textSpan;
        t.style = style;
        t.textAlign = textAlign;
        if (textScaler != null) {
            t.textScaler(textScaler);
        }
        return t;
    }

    /** Flutter's {@code Text.textSpan}: the span of a {@link #rich} text, else null. */
    public TextSpan textSpan() {
        return textSpan;
    }

    public void style(TextStyle v) {
        this.style = v;
    }

    public void semanticsLabel(String v) {
        this.semanticsLabel = v;
    }

    public void overflow(TextOverflow v) {
        this.overflow = v;
    }

    public void maxLines(long v) {
        this.maxLines = v;
    }

    public void softWrap(boolean v) {
    }

    public String getSemanticsLabel() {
        return semanticsLabel;
    }

    public TextOverflow getOverflow() {
        return overflow;
    }

    public Long getMaxLines() {
        return maxLines;
    }

    public void textAlign(TextAlign v) {
        this.textAlign = v;
    }

    public String getData() {
        return data;
    }

    public TextStyle getStyle() {
        return style;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    @Override
    public Element createElement() {
        if (textSpan != null) {
            RichText rt = new RichText();
            TextSpan root = textSpan;
            if (style != null) {
                root = new TextSpan();
                root.style(style);
                root.children(dart.core.DartList.of(textSpan));
            }
            rt.text(root);
            if (textAlign != null) {
                rt.textAlign(textAlign);
            }
            rt.textScaler(textScaler);
            return rt.createElement();
        }
        return new TextRenderElement(this);
    }

    /// Text.data: the string, or null for Text.rich.
    public String data() {
        return data;
    }

    private Object textScaler;

    /// Text.textScaler: the font size is scaled by it (see TextScaler.applyTo).
    public void textScaler(Object v) {
        this.textScaler = v;
    }

    public Object textScaler() {
        return textScaler;
    }
}
