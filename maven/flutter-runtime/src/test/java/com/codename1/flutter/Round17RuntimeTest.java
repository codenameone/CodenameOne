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

import com.codename1.flutter.intl.DateFormat;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.StatefulBuilder;

import dart.core.DateTime;
import dart.runtime.Funcs;

import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// setState in a StatefulBuilder rebuilds it; viewPadding carries the safe area; a UTC
/// time formats as itself even in the device's daylight-saving gap.
class Round17RuntimeTest {

    @Test
    void statefulBuildersSetStateRebuildsIt() {
        final int[] count = {0};
        final int[] builds = {0};
        final Object[] setter = new Object[1];
        StatefulBuilder sb = new StatefulBuilder();
        sb.builder(new Funcs.Func2<BuildContext, Funcs.VoidFunc1<Funcs.VoidFunc0>, Widget>() {
            @Override
            public Widget call(BuildContext c, Funcs.VoidFunc1<Funcs.VoidFunc0> setState) {
                builds[0]++;
                setter[0] = setState;
                return new ProbeBox(1, 1);
            }
        });
        BuildOwner owner = new BuildOwner();
        FlutterUI.mount(sb, new RenderHost(), owner);
        assertEquals(1, builds[0]);
        @SuppressWarnings("unchecked")
        Funcs.VoidFunc1<Funcs.VoidFunc0> setState = (Funcs.VoidFunc1<Funcs.VoidFunc0>) setter[0];
        setState.call(() -> count[0]++);
        owner.flushSync();
        assertEquals(1, count[0]);
        assertEquals(2, builds[0], "the builder ran again after setState");
    }

    @Test
    void viewPaddingCarriesTheSafeArea() {
        MediaQueryData q = new MediaQueryData(new Size(400, 800), 1.0, Brightness.light, 1.0,
                EdgeInsets.only(0, 44, 0, 34));
        assertEquals(44.0, q.viewPadding().top(), 0.0);
        assertEquals(34.0, q.viewPadding().bottom(), 0.0);
    }

    @Test
    void aUtcTimeInTheLocalDaylightSavingGapFormatsAsItself() {
        TimeZone saved = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        try {
            // 02:30 on the day New York springs forward does not exist locally.
            DateTime t = DateTime.utc(2024, 3, 10, 2, 30, 0, 0, 0);
            assertEquals("02:30", DateFormat.Hm(null).format(t));
        } finally {
            TimeZone.setDefault(saved);
        }
    }
}
