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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Key;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;

/**
 * Selectable, non-editable text — Flutter's {@code SelectableText}. Text
 * selection is not yet wired, so this composes a plain {@link Text} (or
 * {@link RichText} for the {@code .rich} constructor), which is faithful to the
 * rendered appearance.
 */
public class SelectableText extends StatelessWidget {

    private final String data;
    private TextSpan textSpan;
    private TextStyle style;
    private TextAlign textAlign;

    public SelectableText(String data) {
        this.data = data;
    }

    /**
     * Dart's {@code SelectableText.rich} named constructor in canonical
     * positional form.
     */
    public static SelectableText rich(TextSpan textSpan, Key key, TextStyle style,
                                      TextAlign textAlign, Long maxLines,
                                      Object textDirection) {
        SelectableText t = new SelectableText(null);
        t.key(key);
        t.textSpan = textSpan;
        t.style = style;
        t.textAlign = textAlign;
        return t;
    }

    public void style(TextStyle v) {
        this.style = v;
    }

    public void textAlign(TextAlign v) {
        this.textAlign = v;
    }

    public void maxLines(long v) {
    }

    public void textScaleFactor(double v) {
    }

    public void showCursor(boolean v) {
    }

    public void semanticsLabel(String v) {
    }

    public void cursorColor(Object v) {
    }

    public void onTap(Object v) {
    }

    public void focusNode(Object v) {
    }

    public void scrollPhysics(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        if (textSpan != null) {
            RichText r = new RichText();
            r.text(textSpan);
            if (textAlign != null) {
                r.textAlign(textAlign);
            }
            return r;
        }
        Text t = new Text(data);
        if (style != null) {
            t.style(style);
        }
        if (textAlign != null) {
            t.textAlign(textAlign);
        }
        return t;
    }
}
