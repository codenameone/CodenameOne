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

import com.codename1.flutter.FocusNode;
import com.codename1.flutter.ValueKey;
import com.codename1.flutter.material.Checkbox;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// This round's runtime fixes that run without a display: a FocusNode drives the
/// component it is attached to, a tristate checkbox reaches null, ValueKey follows
/// Dart's numeric equality, and a controller's initial offset reaches its list.
class Round9RuntimeTest {

    /** A component stand-in that records what the node asked of it. */
    private static final class RecordingHost implements FocusNode.Host {
        final List<String> calls = new ArrayList<String>();

        @Override
        public void focusHost() {
            calls.add("focus");
        }

        @Override
        public void blurHost() {
            calls.add("blur");
        }
    }

    @Test
    void requestFocusAndUnfocusMoveTheAttachedComponent() {
        FocusNode node = new FocusNode();
        RecordingHost host = new RecordingHost();
        node.attachHost(host);
        node.requestFocus();
        assertTrue(node.hasFocus());
        assertEquals("[focus]", host.calls.toString());
        node.unfocus();
        assertFalse(node.hasFocus());
        assertEquals("[focus, blur]", host.calls.toString());
        node.dispose();
    }

    @Test
    void aFocusRequestedBeforeTheComponentExistsReachesItOnAttach() {
        FocusNode node = new FocusNode();
        node.requestFocus();
        RecordingHost host = new RecordingHost();
        node.attachHost(host);
        assertEquals("[focus]", host.calls.toString());
        node.dispose();
    }

    @Test
    void oneNodeHoldsThePrimaryFocusAndTheScopeUnfocusesIt() {
        FocusNode a = new FocusNode();
        FocusNode b = new FocusNode();
        final int[] aNotified = {0};
        a.addListener(() -> aNotified[0]++);
        a.requestFocus();
        b.requestFocus();
        assertFalse(a.hasFocus(), "the previous node loses the focus");
        assertEquals(2, aNotified[0]);
        assertSame(b, FocusNode.primaryFocus());
        FocusScopeNode scope = new FocusScopeNode();
        assertTrue(scope.hasFocus());
        scope.unfocus();
        assertFalse(b.hasFocus());
        assertNull(FocusNode.primaryFocus());
        scope.requestFocus(a);
        assertTrue(a.hasFocus());
        a.dispose();
        b.dispose();
    }

    @Test
    void theComponentReportsFocusItGainsAndLosesByItself() {
        FocusNode node = new FocusNode();
        node.attachHost(new RecordingHost());
        node.hostFocusChanged(true);
        assertTrue(node.hasFocus());
        node.hostFocusChanged(false);
        assertFalse(node.hasFocus());
        node.dispose();
    }

    @Test
    void aTristateCheckboxCyclesThroughNull() {
        Checkbox c = new Checkbox();
        c.tristate(true);
        c.value(Boolean.FALSE);
        assertEquals(Boolean.TRUE, c.nextValue());
        c.value(Boolean.TRUE);
        assertNull(c.nextValue(), "true advances to the indeterminate state");
        c.value(null);
        assertEquals(Boolean.FALSE, c.nextValue(), "indeterminate advances to false");
        c.tristate(false);
        c.value(Boolean.TRUE);
        assertEquals(Boolean.FALSE, c.nextValue());
    }

    @Test
    void valueKeysFollowDartNumericEquality() {
        ValueKey<Number> i = new ValueKey<Number>(Long.valueOf(1));
        ValueKey<Number> d = new ValueKey<Number>(Double.valueOf(1.0));
        assertEquals(i, d);
        assertEquals(i.hashCode(), d.hashCode());
        assertNotEquals(new ValueKey<Number>(Double.valueOf(Double.NaN)),
                new ValueKey<Number>(Double.valueOf(Double.NaN)));
        assertEquals(new ValueKey<Number>(Double.valueOf(0.0)), new ValueKey<Number>(Double.valueOf(-0.0)));
        assertEquals(new ValueKey<Number>(Double.valueOf(0.0)).hashCode(),
                new ValueKey<Number>(Double.valueOf(-0.0)).hashCode());
        assertNotEquals(new ValueKey<Number>(Long.valueOf(1)), new ValueKey<Number>(Double.valueOf(1.5)));
    }

    @Test
    void aControllersInitialOffsetReachesTheListItAttachesTo() {
        ScrollController c = new ScrollController();
        c.initialScrollOffset(120);
        final double[] movedTo = {-1};
        c.attach(new ScrollController.Client() {
            @Override
            public void scrollToOffset(double offset) {
                movedTo[0] = offset;
            }
        });
        assertEquals(120.0, movedTo[0], 0.0);
        assertEquals(120.0, c.offset(), 0.0);
    }
}
