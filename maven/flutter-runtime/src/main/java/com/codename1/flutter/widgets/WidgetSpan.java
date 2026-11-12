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

import com.codename1.flutter.Widget;

/// A widget inline in a span tree -- Flutter's `WidgetSpan`. It extends TextSpan
/// here (Flutter's extends PlaceholderSpan) so it fits the TextSpan children lists
/// this runtime keeps; {@link Text#rich} lays a tree containing one out as a flow
/// of words and widgets (see {@link #flow}).
public class WidgetSpan extends TextSpan {

    private Widget child;
    private Object alignment;
    private Object baseline;

    public void child(Widget v) {
        this.child = v;
    }

    /// Flutter's {@code child}.
    public Widget child() {
        return child;
    }

    public void alignment(Object v) {
        this.alignment = v;
    }

    public Object alignment() {
        return alignment;
    }

    public void baseline(Object v) {
        this.baseline = v;
    }

    /// U+FFFC, the object replacement character, as Flutter's WidgetSpan reports.
    @Override
    public String toPlainText() {
        return "\ufffc";
    }

    /// Whether {@code span}'s tree contains a WidgetSpan.
    static boolean contains(TextSpan span) {
        if (span == null) {
            return false;
        }
        if (span instanceof WidgetSpan) {
            return true;
        }
        dart.core.DartList<TextSpan> kids = span.getChildren();
        if (kids != null) {
            for (int i = 0; i < kids.size(); i++) {
                if (contains(kids.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /// The span tree as a Wrap: each word of text is a Text carrying its resolved
    /// style (so lines break between words, as in a paragraph), and each WidgetSpan
    /// is its widget, vertically centred on the line.
    static Widget flow(TextSpan root, com.codename1.flutter.TextStyle rootStyle,
                       com.codename1.flutter.TextAlign textAlign) {
        dart.core.DartList<Widget> parts =
                new dart.core.DartList<Widget>(dart.runtime.TypeWitness.of(Widget.class, false));
        collect(root, rootStyle, parts);
        Wrap w = new Wrap();
        w.crossAxisAlignment(com.codename1.flutter.WrapCrossAlignment.center);
        if (textAlign == com.codename1.flutter.TextAlign.center) {
            w.alignment(com.codename1.flutter.WrapAlignment.center);
        } else if (textAlign == com.codename1.flutter.TextAlign.right || textAlign == com.codename1.flutter.TextAlign.end) {
            w.alignment(com.codename1.flutter.WrapAlignment.end);
        }
        w.children(parts);
        return w;
    }

    private static void collect(TextSpan span, com.codename1.flutter.TextStyle inherited,
                                dart.core.DartList<Widget> out) {
        if (span == null) {
            return;
        }
        if (span instanceof WidgetSpan) {
            Widget c = ((WidgetSpan) span).child;
            if (c != null) {
                out.add(c);
            }
            return;
        }
        com.codename1.flutter.TextStyle eff = RichTextRenderElement.resolve(inherited, span.getStyle());
        String t = span.getText();
        if (t != null) {
            int i = 0;
            while (i < t.length()) {
                int sp = t.indexOf(' ', i);
                int end = sp < 0 ? t.length() : sp + 1;
                Text word = new Text(t.substring(i, end));
                if (eff != null) {
                    word.style(eff);
                }
                out.add(word);
                i = end;
            }
        }
        dart.core.DartList<TextSpan> kids = span.getChildren();
        if (kids != null) {
            for (int i = 0; i < kids.size(); i++) {
                collect(kids.get(i), eff, out);
            }
        }
    }
}
