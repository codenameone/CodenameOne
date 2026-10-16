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

import com.codename1.flutter.material.Dialogs;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.testsupport.RasterDisplay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ordering inside push and pop, which only the displayed path exercises: a pushed
 * page's first build must already see itself on the stack, and a pop's result must be
 * delivered only once the way back is committed.
 */
class NavigatorOrderingTest {

    @BeforeEach
    void display() {
        RasterDisplay.install();
        Navigator.reset();
        Dialogs.reset();
    }

    @AfterEach
    void noDisplay() {
        Navigator.reset();
        RasterDisplay.uninstall();
    }

    private static MaterialPageRoute<Object> page(final List<Boolean> canPopDuringBuild) {
        MaterialPageRoute<Object> r = new MaterialPageRoute<Object>();
        r.builder(context -> {
            if (canPopDuringBuild != null) {
                canPopDuringBuild.add(Navigator.of(context, Boolean.FALSE).canPop());
            }
            return new ProbeBox(10, 10);
        });
        return r;
    }

    @Test
    void aPushedPageSeesItselfPoppableDuringItsFirstBuild() {
        List<Boolean> seen = new ArrayList<Boolean>();
        Navigator.push(null, page(seen));
        assertTrue(!seen.isEmpty(), "the page built");
        assertEquals(Boolean.TRUE, seen.get(0),
                "an AppBar's automaticallyImplyLeading asks this during the first build");
    }

    /// A regression check for the stack and the predecessor only: this display finishes
    /// showBack() synchronously and never makes a forward show current, so it cannot
    /// reproduce the in-flight return transition the completion order protects.
    @Test
    void aPushFromThePopResultCallbackSitsAboveTheRemainingPage() {
        Navigator.push(null, page(null));
        com.codename1.ui.Form first = Navigator.formAt(0);
        dart.async.Future<Object> second = Navigator.push(null, page(null));
        final List<Integer> sizeAtCallback = new ArrayList<Integer>();
        second.then((dart.runtime.Funcs.VoidFunc1<Object>) v -> {
            sizeAtCallback.add(Navigator.stackSize());
            Navigator.push(null, page(null));
        });

        Navigator.pop(null, "done");

        assertEquals(java.util.Arrays.asList(1), sizeAtCallback, "the popped route is gone when its future completes");
        assertEquals(2, Navigator.stackSize(), "first page plus the one pushed from the callback");
        assertTrue(Navigator.previousFormOfTop() == first,
                "it returns to the first page, not to the page that was being popped");
    }

    /// A displayed dialog is disposed BEFORE its future completes, so a continuation that
    /// pops the page beneath runs with the dialog already gone.
    @Test
    void aDialogIsGoneBeforeItsResultIsDelivered() {
        Navigator.push(null, page(null));
        // A repaint already queued when the dialog shows: Dialog.showPacked flushes the EDT,
        // which paints, and CI's test order happened to leave one where the local run did
        // not. Queued here so the paint path always runs.
        Navigator.formAt(0).repaint();
        final List<Integer> openAtCallback = new ArrayList<Integer>();
        final List<Boolean> showingAtCallback = new ArrayList<Boolean>();
        dart.async.Future<Object> result = Dialogs.showDialog(null, (context) -> new ProbeBox(5, 5));
        result.then((dart.runtime.Funcs.VoidFunc1<Object>) v -> {
            openAtCallback.add(Dialogs.openDialogCount());
            com.codename1.ui.Form now = com.codename1.ui.Display.getInstance().getCurrent();
            showingAtCallback.add(now instanceof com.codename1.ui.Dialog && now.isVisible());
        });

        Navigator.pop(null, "ok");

        assertEquals(java.util.Arrays.asList(0), openAtCallback, "off the dialog stack");
        assertEquals(java.util.Arrays.asList(Boolean.FALSE), showingAtCallback,
                "and no longer the shown dialog when the result arrives");
        assertEquals(1, Navigator.stackSize(), "the page beneath is untouched");
    }
}
