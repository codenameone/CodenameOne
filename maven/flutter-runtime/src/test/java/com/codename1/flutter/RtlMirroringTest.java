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

package com.codename1.flutter;

import com.codename1.flutter.l10n.MaterialLocalizations;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Align;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.Directionality;
import com.codename1.flutter.widgets.Row;
import com.codename1.flutter.widgets.Wrap;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Layouts that run along the reading direction mirror under right-to-left text, as
/// Flutter's do, and the framework strings follow the locale's language.
class RtlMirroringTest {

    private static RenderElement layoutUnder(TextDirection d, Widget child) {
        Directionality dir = new Directionality();
        dir.textDirection(d);
        dir.child(child);
        RenderHost host = new RenderHost();
        FlutterUI.mount(dir, host, new BuildOwner());
        RenderElement r = host.rootRenderElement();
        r.layout(BoxConstraints.tight(400, 600));
        r.position(0, 0);
        return r;
    }

    private static Row row() {
        Row row = new Row();
        row.children(DartList.of(new ProbeBox(50, 20), new ProbeBox(30, 20)));
        return row;
    }

    @Test
    void rowRunsFromTheRightUnderRtl() {
        RenderElement ltr = layoutUnder(TextDirection.ltr, row());
        assertEquals(0, ltr.renderChildren().get(0).x());
        assertEquals(50, ltr.renderChildren().get(1).x());
        RenderElement rtl = layoutUnder(TextDirection.rtl, row());
        assertEquals(350, rtl.renderChildren().get(0).x());
        assertEquals(320, rtl.renderChildren().get(1).x());
    }

    @Test
    void rowEndIsTheLeftEdgeUnderRtl() {
        Row r = row();
        r.mainAxisAlignment(MainAxisAlignment.end);
        RenderElement rtl = layoutUnder(TextDirection.rtl, r);
        assertEquals(30, rtl.renderChildren().get(0).x());
        assertEquals(0, rtl.renderChildren().get(1).x());
    }

    @Test
    void columnCrossStartIsTheRightEdgeUnderRtl() {
        Column c = new Column();
        c.crossAxisAlignment(CrossAxisAlignment.start);
        c.children(DartList.of(new ProbeBox(50, 20), new Row()));
        Column outer = c;
        RenderElement rtl = layoutUnder(TextDirection.rtl, outer);
        RenderElement first = rtl.renderChildren().get(0);
        assertEquals(rtl.size().width() - 50, first.x(), 0.5);
    }

    @Test
    void alignmentDirectionalStartIsTheRightEdgeUnderRtl() {
        Align a = new Align();
        a.alignment(AlignmentDirectional.centerStart);
        a.child(new ProbeBox(40, 20));
        assertEquals(360, layoutUnder(TextDirection.rtl, a).renderChildren().get(0).x());
        Align b = new Align();
        b.alignment(Alignment.centerLeft);
        b.child(new ProbeBox(40, 20));
        assertEquals(0, layoutUnder(TextDirection.rtl, b).renderChildren().get(0).x(),
                "a plain Alignment never mirrors");
    }

    @Test
    void wrapFillsFromTheRightUnderRtl() {
        Wrap w = new Wrap();
        w.children(DartList.of(new ProbeBox(50, 20), new ProbeBox(30, 20)));
        RenderElement rtl = layoutUnder(TextDirection.rtl, w);
        assertEquals(rtl.size().width() - 50, rtl.renderChildren().get(0).x(), 0.5);
    }

    @Test
    void materialStringsFollowTheLanguage() {
        assertEquals("Cancel", MaterialLocalizations.forLocale(new Locale("en")).cancelButtonLabel());
        assertEquals("Cancelar", MaterialLocalizations.forLocale(new Locale("es")).cancelButtonLabel());
        assertEquals("\u05d1\u05d9\u05d8\u05d5\u05dc",
                MaterialLocalizations.forLocale(new Locale("he")).cancelButtonLabel());
        assertEquals("OK", MaterialLocalizations.forLocale(new Locale("fr")).okButtonLabel());
        assertEquals(1, MaterialLocalizations.forLocale(new Locale("es")).firstDayOfWeekIndex());
    }
}
