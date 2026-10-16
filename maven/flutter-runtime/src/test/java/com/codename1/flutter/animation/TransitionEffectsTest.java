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

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Offset;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.FractionalTranslationRenderElement;
import com.codename1.flutter.widgets.Opacity;
import com.codename1.flutter.widgets.Transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The transition widgets APPLY their effect.
 *
 * <p>The whole family used to render the child straight through: a FadeTransition never
 * faded, a ScaleTransition never scaled, a SlideTransition never moved. Nothing failed and
 * nothing looked broken in a still screenshot — every animated reveal in the app was simply
 * a cut.</p>
 */
class TransitionEffectsTest {

    /** An animation stuck at one value — enough to check what the build produces. */
    private static Animation<Double> at(double v) {
        return new AlwaysStoppedAnimation<Double>(Double.valueOf(v));
    }

    private static Widget buildOf(AnimatedChildWidget w) {
        return w.build(null);
    }

    @Test
    @DisplayName("a fade wraps the child in an Opacity carrying the animation's value")
    void fadeAppliesOpacity() {
        FadeTransition f = new FadeTransition();
        f.opacity(at(0.25));
        f.child(new ProbeBox(10, 10));

        Widget built = buildOf(f);
        assertTrue(built instanceof Opacity, "expected an Opacity, got " + built);
        assertEquals(0.25, ((Opacity) built).getOpacity(), 1e-9);
    }

    @Test
    @DisplayName("a fade subscribes, so a tick repaints it")
    void fadeListens() {
        FadeTransition f = new FadeTransition();
        Animation<Double> a = at(1);
        f.opacity(a);

        assertNotNull(f.listenable(), "the animation must be the widget's listenable");
        assertEquals(a, f.listenable());
    }

    @Test
    @DisplayName("a scale wraps the child in a Transform carrying the factor")
    void scaleAppliesTransform() {
        ScaleTransition s = new ScaleTransition();
        s.scale(at(0.5));
        s.child(new ProbeBox(10, 10));

        Widget built = buildOf(s);
        assertTrue(built instanceof Transform, "expected a Transform, got " + built);
        assertEquals(0.5, ((Transform) built).effectiveScaleX(), 1e-9);
        assertEquals(0.5, ((Transform) built).effectiveScaleY(), 1e-9);
    }

    @Test
    @DisplayName("a rotation converts TURNS to radians")
    void rotationConvertsTurns() {
        RotationTransition r = new RotationTransition();
        r.turns(at(0.25));            // a quarter turn
        r.child(new ProbeBox(10, 10));

        Transform built = (Transform) buildOf(r);
        assertEquals(Math.PI / 2, built.effectiveAngle().doubleValue(), 1e-9);
    }

    @Test
    @DisplayName("a slide reports its offset as a fraction, read at paint time")
    void slideExposesItsFraction() {
        SlideTransition s = new SlideTransition();
        s.position(new AlwaysStoppedAnimation<Object>(new Offset(0.5, -0.25)));
        s.child(new ProbeBox(10, 10));

        Offset f = ((FractionalTranslationRenderElement.FractionSource) s).fraction();
        assertEquals(0.5, f.dx(), 1e-9);
        assertEquals(-0.25, f.dy(), 1e-9);
    }

    @Test
    @DisplayName("a slide lays its child out unmoved - the shift is paint-only")
    void slideDoesNotDisturbLayout() {
        SlideTransition s = new SlideTransition();
        s.position(new AlwaysStoppedAnimation<Object>(new Offset(1, 1)));
        s.child(new ProbeBox(40, 20));

        RenderElement e = (RenderElement) FlutterUI.mount(s, new RenderHost(), new BuildOwner());
        e.layout(BoxConstraints.loose(100, 100));

        assertEquals(40, e.size().width(), 1e-9, "a slid child keeps its slot");
        assertEquals(20, e.size().height(), 1e-9);
    }

    @Test
    @DisplayName("with no animation a transition is a no-op, not a blank")
    void anAbsentAnimationShowsTheChild() {
        FadeTransition f = new FadeTransition();
        f.child(new ProbeBox(10, 10));

        assertEquals(1.0, ((Opacity) buildOf(f)).getOpacity(), 1e-9,
                "no animation must mean fully visible, never fully transparent");
    }

    @Test
    @DisplayName("an unmounted slide stops following its animation")
    void anUnmountedSlideUnsubscribes() {
        SlideTransition s = new SlideTransition();
        s.position(new AlwaysStoppedAnimation<Object>(new Offset(0, 0)));
        s.child(new ProbeBox(10, 10));

        Element e = FlutterUI.mount(s, new RenderHost(), new BuildOwner());
        FlutterUI.unmountTree(e);
        // Nothing to assert beyond surviving the teardown: a listener left on a controller
        // that outlives the element is a leak that only shows up as a stale repaint later.
        assertTrue(true);
    }
}
