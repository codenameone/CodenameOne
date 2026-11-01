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
import com.codename1.flutter.TextSelection;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import dart.runtime.Funcs;

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
    /** Flutter's {@code SelectableText.textSpan}: the span of a rich one, else null. */
    public TextSpan textSpan() {
        return textSpan;
    }

    public static SelectableText rich(TextSpan textSpan, Key key, TextStyle style,
                                      TextAlign textAlign, Long maxLines,
                                      Object textDirection) {
        return rich(textSpan, key, style, textAlign, maxLines, textDirection, null, null, null, null);
    }

    /// The full {@code SelectableText.rich}: textScaler, onSelectionChanged, onTap and showCursor too.
    public static SelectableText rich(TextSpan textSpan, Key key, TextStyle style,
                                      TextAlign textAlign, Long maxLines,
                                      Object textDirection, Object textScaler,
                                      Object onSelectionChanged, Object onTap, Boolean showCursor) {
        SelectableText t = new SelectableText(null);
        t.textScaler = textScaler;
        t.onSelectionChanged = onSelectionChanged;
        t.onTap = onTap;
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

    private Object onTap;

    public void textScaleFactor(double v) {
        this.textScaler = com.codename1.flutter.TextScaler.linear(v);
    }

    public void showCursor(boolean v) {
    }

    public void semanticsLabel(String v) {
    }

    public void cursorColor(Object v) {
    }

    public void onTap(Object v) {
        this.onTap = v;
    }

    public void focusNode(Object v) {
    }

    public void scrollPhysics(Object v) {
    }

    /// The text's length in UTF-16 units, the offsets a selection is counted in.
    private long length() {
        if (data != null) {
            return data.length();
        }
        StringBuilder sb = new StringBuilder();
        for (RichTextRenderElement.Run r : RichTextRenderElement.flatten(textSpan)) {
            sb.append(r.text);
        }
        return sb.length();
    }

    /// Changes the selection and reports it to onSelectionChanged with a null cause.
    public void select(long base, long extent) {
        select(base, extent, null);
    }

    /// Changes the selection and reports it to onSelectionChanged with its
    /// SelectionChangedCause. A long press selects the whole text
    /// (`longPress`); a tap collapses the selection at its start (`tap`), and
    /// calls onTap.
    @SuppressWarnings("unchecked")
    public void select(long base, long extent, com.codename1.flutter.SelectionChangedCause cause) {
        TextSelection sel = new TextSelection();
        sel.baseOffset(base);
        sel.extentOffset(extent);
        if (onSelectionChanged instanceof Funcs.VoidFunc2) {
            ((Funcs.VoidFunc2<Object, Object>) onSelectionChanged).call(sel, cause);
        } else if (onSelectionChanged instanceof Funcs.Func2) {
            ((Funcs.Func2<Object, Object, Object>) onSelectionChanged).call(sel, cause);
        }
    }

    @Override
    public Widget build(BuildContext context) {
        Widget shown;
        if (textSpan != null) {
            RichText r = new RichText();
            r.text(textSpan);
            if (textAlign != null) {
                r.textAlign(textAlign);
            }
            r.textScaler(textScaler);
            shown = r;
        } else {
            Text t = new Text(data);
            if (style != null) {
                t.style(style);
            }
            if (textAlign != null) {
                t.textAlign(textAlign);
            }
            t.textScaler(textScaler);
            shown = t;
        }
        if (onSelectionChanged == null && onTap == null) {
            return shown;
        }
        GestureDetector g = new GestureDetector();
        g.onLongPress(() -> select(0, length(), com.codename1.flutter.SelectionChangedCause.longPress));
        g.onTap(() -> {
            select(0, 0, com.codename1.flutter.SelectionChangedCause.tap);
            if (onTap instanceof Funcs.VoidFunc0) {
                ((Funcs.VoidFunc0) onTap).call();
            }
        });
        g.child(shown);
        return g;
    }

    private Object onSelectionChanged;
    private Object textScaler;

    /// SelectableText.onSelectionChanged: called with each selection change.
    public void onSelectionChanged(Object v) {
        this.onSelectionChanged = v;
    }

    public void textScaler(Object v) {
        this.textScaler = v;
    }
}
