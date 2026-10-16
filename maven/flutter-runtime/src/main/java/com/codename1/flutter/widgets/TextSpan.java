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

import com.codename1.flutter.TextStyle;

import dart.core.DartList;

/**
 * A node in a styled-text tree (Flutter's TextSpan): an optional text run,
 * an optional style, and optional child spans. A child span INHERITS every
 * style property its own style leaves null from its parent chain (see
 * {@link RichTextRenderElement#flatten}).
 *
 * <p>Not a Widget — it is configuration consumed by {@link RichText}.</p>
 */
public class TextSpan extends InlineSpan {

    private String text;
    private TextStyle style;
    private DartList<TextSpan> children;
    private Object recognizer;

    public void text(String v) {
        this.text = v;
    }

    public void recognizer(Object v) {
        this.recognizer = v;
    }

    public Object getRecognizer() {
        return recognizer;
    }

    public void style(TextStyle v) {
        this.style = v;
    }

    public void children(DartList<TextSpan> v) {
        this.children = v;
    }

    public String getText() {
        return text;
    }

    public TextStyle getStyle() {
        return style;
    }

    public DartList<TextSpan> getChildren() {
        return children;
    }

    /**
     * {@code InlineSpan.toPlainText}: the concatenated raw text of this span and
     * all descendant spans, in depth-first order.
     */
    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        appendPlainText(sb);
        return sb.toString();
    }

    private void appendPlainText(StringBuilder sb) {
        if (text != null) {
            sb.append(text);
        }
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                TextSpan c = children.get(i);
                if (c != null) {
                    c.appendPlainText(sb);
                }
            }
        }
    }
}
