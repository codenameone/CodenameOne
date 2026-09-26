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
package com.codename1.flutter.animation;

import com.codename1.flutter.FlutterErrorReport;
import com.codename1.flutter.foundation.ValueNotifier;
import dart.runtime.Funcs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A listener that throws is reported, and every other listener still runs --
/// on an animation, where the throw used to reach the frame clock and drop the
/// whole controller, and on the plain notifiers that share the dispatch.
class ListenerIsolationTest {

    @BeforeEach
    @AfterEach
    void clearReports() {
        FlutterErrorReport.reset();
    }

    private static Funcs.VoidFunc0 thrower() {
        return new Funcs.VoidFunc0() {
            @Override
            public void call() {
                throw new IllegalStateException("faulty listener");
            }
        };
    }

    private static Funcs.VoidFunc0 counter(final int[] count) {
        return new Funcs.VoidFunc0() {
            @Override
            public void call() {
                count[0]++;
            }
        };
    }

    private static boolean reported() {
        for (FlutterErrorReport.Entry e : FlutterErrorReport.entries()) {
            if ("faulty listener".equals(e.message())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void aThrowingValueListenerDoesNotStopTheOthersOrTheController() {
        AnimationController c = new AnimationController();
        int[] after = {0};
        c.addListener(thrower());
        c.addListener(counter(after));
        c.value(0.5);   // must not throw out of the controller
        assertEquals(1, after[0], "the listener after the faulty one still ran");
        assertEquals(0.5, c.value(), 0.0);
        assertTrue(reported(), "the failure is reported, not swallowed");
    }

    @Test
    void aThrowingStatusListenerDoesNotStopTheOthers() {
        AnimationController c = new AnimationController();
        final int[] after = {0};
        c.addStatusListener(new Funcs.VoidFunc1<AnimationStatus>() {
            @Override
            public void call(AnimationStatus s) {
                throw new IllegalStateException("faulty listener");
            }
        });
        c.addStatusListener(new Funcs.VoidFunc1<AnimationStatus>() {
            @Override
            public void call(AnimationStatus s) {
                after[0]++;
            }
        });
        c.value(1.0);   // dismissed -> completed
        assertEquals(1, after[0]);
        assertTrue(reported());
    }

    @Test
    void theSameHoldsForAValueNotifier() {
        ValueNotifier<Object> n = new ValueNotifier<Object>("a");
        int[] after = {0};
        n.addListener(thrower());
        n.addListener(counter(after));
        n.value("b");
        assertEquals(1, after[0]);
        assertTrue(reported());
    }
}
