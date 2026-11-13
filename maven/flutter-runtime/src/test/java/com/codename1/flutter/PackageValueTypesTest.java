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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.codename1.flutter.vectormath.Vector2;
import com.codename1.flutter.widgets.Icon;
import com.codename1.flutter.widgets.SelectableText;
import dart.runtime.Funcs;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// Value types package code reads directly: vector_math's Vector2, dart:ui's
/// FontFeature, Icon's getters (a subclass of Icon reads its own fields through
/// them) and the cause SelectableText reports a selection change with.
class PackageValueTypesTest {

    @Test
    void vector2MutatesInPlaceAndCloneIsIndependent() {
        Vector2 v = new Vector2(3, 4);
        assertEquals(5.0, v.length(), 1e-9);
        Vector2 c = v.clone();
        c.add(Vector2.all(1));
        assertEquals(3.0, v.x(), 0);
        assertEquals(5.0, c.y(), 0);
        c.multiply(Vector2.all(-1));
        assertEquals(-4.0, c.x(), 0);
        c.divide(Vector2.all(2));
        assertEquals(-2.5, c.y(), 0);
        c.setValues(6, 8);
        assertEquals(10.0, c.normalize(), 1e-9);
        assertEquals(1.0, c.length(), 1e-9);
        c.setZero();
        assertEquals(Vector2.zero(), c);
        assertEquals(new Vector2(4, 6), new Vector2(1, 2).$plus(new Vector2(3, 4)));
        assertEquals(Math.PI, Vector2.radians(180), 1e-12);
    }

    @Test
    void fontFeatureDefaultsOnAndCopyWithRecordsIt() {
        assertEquals(new FontFeature("sups", 1), FontFeature.enable("sups"));
        assertEquals(1L, new FontFeature("liga").value());
        assertEquals(0L, FontFeature.disable("liga").value());
        List<Object> features = new ArrayList<Object>();
        features.add(FontFeature.superscripts());
        TextStyle base = new TextStyle();
        TextStyle t = base.copyWith(null, null, null, null, 20.0, null, null, null, null, null, null, null,
                null, null, features, null, null, null, null);
        assertSame(features, t.fontFeatures());
        assertEquals(20.0, t.getFontSize().doubleValue(), 0);
        assertNull(base.fontFeatures());
    }

    @Test
    void iconExposesItsFieldsAsGetters() {
        Icon i = new Icon(null);
        assertNull(i.size());
        i.size(30);
        i.applyTextScaling(true);
        i.weight(500);
        assertEquals(30.0, i.size().doubleValue(), 0);
        assertEquals(Boolean.TRUE, i.applyTextScaling());
        assertEquals(500.0, i.weight().doubleValue(), 0);
        assertNull(i.textDirection());
        assertNull(i.icon());
    }

    @Test
    void selectableTextReportsTheSelectionCause() {
        final List<Object> causes = new ArrayList<Object>();
        SelectableText t = SelectableText.rich(null, null, null, null, null, null, null,
                (Funcs.VoidFunc2<Object, Object>) (sel, cause) -> causes.add(cause), null, null);
        t.select(0, 3, SelectionChangedCause.longPress);
        t.select(0, 0);
        assertEquals(SelectionChangedCause.longPress, causes.get(0));
        assertNull(causes.get(1));
    }

    @Test
    void anUnsetScalerFollowsNoContextUnchanged() {
        TextStyle s = new TextStyle();
        assertSame(s, TextScaler.applyTo(null, s, null));
        assertNotSame(s, TextScaler.applyTo(TextScaler.linear(2), s, null));
    }
}
