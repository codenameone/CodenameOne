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

import com.codename1.flutter.animation.AnimationController;
import com.codename1.flutter.animation.AnimationStatus;
import com.codename1.flutter.animation.IntTween;
import com.codename1.flutter.animation.ProxyAnimation;
import com.codename1.flutter.animation.ReverseAnimation;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Geometry and tween behaviour Flutter defines exactly, and animations derived from
/// another one forwarding its changes to their own listeners.
class Round13RuntimeTest {

    @Test
    void aNaNRectIsNotFinite() {
        assertFalse(Rect.fromLTRB(Double.NaN, 0, 10, 10).isFinite());
        assertFalse(Rect.fromLTRB(0, 0, Double.POSITIVE_INFINITY, 10).isFinite());
        assertTrue(Rect.fromLTRB(0, 0, 10, 10).isFinite());
    }

    @Test
    void aNullBorderRadiusEndIsZero() {
        BorderRadius r = BorderRadius.circular(10);
        assertEquals(2.5, BorderRadius.lerp(null, r, 0.25).topLeft().x(), 1e-9, "null -> r scales by t");
        assertEquals(7.5, BorderRadius.lerp(r, null, 0.25).topLeft().x(), 1e-9, "r -> null scales by 1 - t");
    }

    @Test
    void intTweenRoundsToTheNearest() {
        IntTween t = new IntTween();
        t.begin(0);
        t.end(1);
        assertEquals(Integer.valueOf(1), t.lerp(0.6));
        assertEquals(Integer.valueOf(0), t.lerp(0.4));
        t.begin(0);
        t.end(-1);
        assertEquals(Integer.valueOf(-1), t.lerp(0.6), "rounding, not truncation toward zero");
    }

    @Test
    void aReverseAnimationNotifiesItsListenersAndReversesStatus() {
        AnimationController parent = new AnimationController();
        ReverseAnimation reverse = new ReverseAnimation(parent.view());
        final List<Double> values = new ArrayList<Double>();
        final List<AnimationStatus> statuses = new ArrayList<AnimationStatus>();
        Funcs.VoidFunc0 onValue = new Funcs.VoidFunc0() {
            @Override
            public void call() {
                values.add(reverse.value());
            }
        };
        reverse.addListener(onValue);
        reverse.addStatusListener(new Funcs.VoidFunc1<AnimationStatus>() {
            @Override
            public void call(AnimationStatus s) {
                statuses.add(s);
            }
        });
        parent.value(0.25);
        assertEquals(0.75, values.get(values.size() - 1), 1e-9);
        parent.forward();
        assertTrue(statuses.contains(AnimationStatus.dismissed), "completed forward reads as dismissed: " + statuses);
        reverse.removeListener(onValue);
        int seen = values.size();
        parent.value(0.5);
        assertEquals(seen, values.size(), "a removed listener hears nothing");
    }

    @Test
    void aProxyAnimationForwardsItsParentAndAParentSwap() {
        AnimationController a = new AnimationController();
        AnimationController b = new AnimationController();
        b.value(1.0);
        ProxyAnimation proxy = new ProxyAnimation(a.view());
        final int[] notified = {0};
        proxy.addListener(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                notified[0]++;
            }
        });
        a.value(0.5);
        assertEquals(1, notified[0], "the parent's change reaches the proxy's listener");
        proxy.parent(b.view());
        assertEquals(2, notified[0], "swapping to a parent at another value is a change");
        a.value(0.1);
        assertEquals(2, notified[0], "the old parent is no longer followed");
        b.value(0.2);
        assertEquals(3, notified[0]);
    }
}
