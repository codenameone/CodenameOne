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
package com.codename1.flutter.navigation;

import java.util.ArrayList;
import java.util.List;

import com.codename1.flutter.material.Dialogs;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.PopScope;
import com.codename1.flutter.widgets.WillPopScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** PopScope and WillPopScope veto maybePop; pop itself is never vetoed. */
class PopScopeTest {

    @BeforeEach
    void resetStacks() {
        Navigator.reset();
        Dialogs.reset();
    }

    /** Pushes a route and mounts {@code page} as its tree, which headless pushes never do. */
    private static void pushMounted(com.codename1.flutter.Widget page) {
        MaterialPageRoute r = new MaterialPageRoute();
        r.builder((context) -> new ProbeBox(1, 1));
        Navigator.push(null, r);
        Navigator.attachTopRootForTest(com.codename1.flutter.FlutterUI.mount(page,
                new com.codename1.flutter.rendering.RenderHost(), new com.codename1.flutter.BuildOwner()));
    }

    private static PopScope<Object> scope(boolean canPop, final List<String> heard) {
        PopScope<Object> scope = new PopScope<Object>();
        scope.canPop(canPop);
        scope.onPopInvokedWithResult((didPop, result) -> heard.add(didPop + ":" + result));
        scope.child(new ProbeBox(10, 10));
        return scope;
    }

    @Test
    void canPopFalseVetoesMaybePopAndReportsTheAttempt() {
        List<String> heard = new ArrayList<String>();
        pushMounted(scope(false, heard));
        assertEquals(Boolean.TRUE, Navigator.maybePop(null, "x").getNow(), "a veto is a handled request");
        assertEquals(1, Navigator.stackSize());
        assertEquals("[false:x]", heard.toString());

        Navigator.pop(null);
        assertEquals(0, Navigator.stackSize(), "pop is not a request and is never vetoed");
    }

    @Test
    void canPopTrueLetsMaybePopCloseAndReportsDidPop() {
        List<String> heard = new ArrayList<String>();
        pushMounted(scope(true, heard));
        assertEquals(Boolean.TRUE, Navigator.maybePop(null, "done").getNow());
        assertEquals(0, Navigator.stackSize());
        assertEquals("[true:done]", heard.toString());
    }

    @Test
    void willPopScopeAnswersAsynchronously() {
        final dart.async.Completer<Boolean> answer = new dart.async.Completer<Boolean>();
        WillPopScope w = new WillPopScope();
        w.onWillPop(() -> answer.future());
        w.child(new ProbeBox(10, 10));
        pushMounted(w);
        dart.async.Future<Boolean> popped = Navigator.maybePop(null, null);
        assertEquals(1, Navigator.stackSize(), "waits for onWillPop");
        answer.complete(Boolean.TRUE);
        assertEquals(Boolean.TRUE, popped.getNow());
        assertEquals(0, Navigator.stackSize());
    }
}
