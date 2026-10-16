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
package com.codename1.flutter.animations;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.material.Dialogs;
import com.codename1.flutter.navigation.Navigator;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * onClosed used to fire only from the builder's close callback, always with null; a
 * page closed by the back button or a plain Navigator.pop never reported at all.
 */
class OpenContainerClosedTest {

    @BeforeEach
    void reset() {
        Navigator.reset();
        Dialogs.reset();
    }

    @Test
    void aPlainPopReportsOnceWithThePagesResult() {
        final List<Object> closed = new ArrayList<Object>();
        OpenContainer<Object> oc = new OpenContainer<Object>();
        oc.closedBuilder((ctx, open) -> new ProbeBox(10, 10));
        oc.openBuilder((ctx, close) -> new ProbeBox(20, 20));
        oc.onClosed((Funcs.VoidFunc1<Object>) closed::add);
        Element e = FlutterUI.mount(oc, new RenderHost(), new BuildOwner());

        oc.open(e);
        assertEquals(1, Navigator.stackSize());
        Navigator.pop(null, "picked");

        assertEquals(java.util.Arrays.asList((Object) "picked"), closed);
    }
}
