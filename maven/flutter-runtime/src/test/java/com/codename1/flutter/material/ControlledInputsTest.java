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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.rendering.RenderHost;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Controlled semantics of the M3 input widgets, headless: a user gesture
 * fires onChanged with the attempted value but the CONFIGURED value stays
 * authoritative until the app rebuilds the widget; Radio selection derives
 * from groupValue equality; Slider scales its double range onto the int
 * progress model losslessly (within a step).
 */
class ControlledInputsTest {

    private static <E extends com.codename1.flutter.Element> E mount(com.codename1.flutter.Widget w) {
        RenderHost host = new RenderHost();
        @SuppressWarnings("unchecked")
        E e = (E) FlutterUI.mount(w, host, new BuildOwner());
        return e;
    }

    // ------------------------------------------------------------------
    // Checkbox
    // ------------------------------------------------------------------

    @Test
    void checkboxToggleFiresOnChangedButValueStaysUntilRebuild() {
        final List<Boolean> received = new ArrayList<Boolean>();
        Checkbox cb = new Checkbox();
        cb.value(false);
        cb.onChanged(received::add);

        CheckboxRenderElement e = mount(cb);
        assertFalse(e.configuredValue());

        e.userToggled(true);
        assertEquals(List.of(true), received, "onChanged got the attempted value");
        assertFalse(e.configuredValue(), "the widget's value is authoritative until a rebuild");

        // the app's setState/rebuild delivers a new widget with the new value
        Checkbox updated = new Checkbox();
        updated.value(true);
        updated.onChanged(received::add);
        e.update(updated);
        assertTrue(e.configuredValue(), "the rebuild moved the checkbox");
    }

    @Test
    void checkboxWithoutOnChangedStillSnapsBack() {
        Checkbox cb = new Checkbox();
        cb.value(true);
        CheckboxRenderElement e = mount(cb);
        e.userToggled(false);
        assertTrue(e.configuredValue());
    }

    // ------------------------------------------------------------------
    // Switch
    // ------------------------------------------------------------------

    @Test
    void switchToggleFiresOnChangedButValueStaysUntilRebuild() {
        final List<Boolean> received = new ArrayList<Boolean>();
        Switch sw = new Switch();
        sw.value(true);
        sw.onChanged(received::add);

        SwitchRenderElement e = mount(sw);
        e.userToggled(false);
        assertEquals(List.of(false), received);
        assertTrue(e.configuredValue(), "still on until the app rebuilds");

        Switch updated = new Switch();
        updated.value(false);
        e.update(updated);
        assertFalse(e.configuredValue());
    }

    // ------------------------------------------------------------------
    // Radio
    // ------------------------------------------------------------------

    @Test
    void radioSelectionDerivesFromGroupValueEquality() {
        Radio vanilla = new Radio();
        vanilla.value("vanilla");
        vanilla.groupValue("chocolate");
        RadioRenderElement e = mount(vanilla);
        assertFalse(e.selected());

        Radio nowSelected = new Radio();
        nowSelected.value("vanilla");
        // a fresh but EQUAL string: Dart == semantics, not identity
        nowSelected.groupValue(new StringBuilder("vanilla").toString());
        e.update(nowSelected);
        assertTrue(e.selected());
    }

    @Test
    void radioSelectFiresOnChangedWithItsValueAndStaysControlled() {
        final List<Object> received = new ArrayList<Object>();
        Radio r = new Radio();
        r.value("vanilla");
        r.groupValue("chocolate");
        r.onChanged(received::add);

        RadioRenderElement e = mount(r);
        e.userSelected();
        assertEquals(List.of("vanilla"), received, "onChanged reports this radio's value");
        assertFalse(e.selected(), "selection only moves when groupValue does");
    }

    // ------------------------------------------------------------------
    // Slider
    // ------------------------------------------------------------------

    @Test
    void sliderScalingRoundTripsWithinAStep() {
        double min = -2.0;
        double max = 6.0;
        long steps = 16;
        for (int p = 0; p <= steps; p++) {
            double v = SliderRenderElement.valueFor(p, min, max, steps);
            assertEquals(p, SliderRenderElement.progressFor(v, min, max, steps),
                    "progress -> value -> progress is exact at " + p);
        }
        // value -> progress -> value stays within half a step
        double stepSize = (max - min) / steps;
        for (double v = min; v <= max; v += 0.37) {
            int p = SliderRenderElement.progressFor(v, min, max, steps);
            double back = SliderRenderElement.valueFor(p, min, max, steps);
            assertTrue(Math.abs(back - v) <= stepSize / 2 + 1e-9,
                    "roundtrip drift at " + v + " -> " + back);
        }
    }

    @Test
    void sliderScalingClampsOutOfRangeAndDegenerateRanges() {
        assertEquals(0, SliderRenderElement.progressFor(-5, 0, 1, 10));
        assertEquals(10, SliderRenderElement.progressFor(7, 0, 1, 10));
        assertEquals(0, SliderRenderElement.progressFor(3, 4, 4, 10), "max <= min collapses to 0");
        assertEquals(0.0, SliderRenderElement.valueFor(5, 0.0, 1.0, 0), "no steps returns min");
    }

    @Test
    void sliderDragFiresScaledDoubleAndStaysControlled() {
        final List<Double> received = new ArrayList<Double>();
        Slider s = new Slider();
        s.value(2.0);
        s.min(0.0);
        s.max(10.0);
        s.divisions(20L);
        s.onChanged(received::add);

        SliderRenderElement e = mount(s);
        assertEquals(20, e.steps());
        assertEquals(4, e.configuredProgress(), "2.0 in [0,10] over 20 steps");

        e.userDragged(15);
        assertEquals(List.of(7.5), received, "progress 15/20 of [0,10]");
        assertEquals(4, e.configuredProgress(), "configured value did not move");
    }

    @Test
    void sliderDefaultsMatchFlutter() {
        Slider s = new Slider();
        assertEquals(0.0, s.getMin());
        assertEquals(1.0, s.getMax());
        SliderRenderElement e = mount(s);
        assertEquals(SliderRenderElement.DEFAULT_STEPS, e.steps(), "continuous default");
    }

    // ------------------------------------------------------------------
    // TextField / controller (headless flow)
    // ------------------------------------------------------------------

    @Test
    void textFieldUserEditSyncsControllerAndFiresOnChanged() {
        final List<String> changed = new ArrayList<String>();
        final int[] notified = {0};
        TextEditingController ctl = new TextEditingController();
        ctl.text("start");
        ctl.addListener(() -> notified[0]++);

        TextField tf = new TextField();
        tf.controller(ctl);
        tf.onChanged(changed::add);

        TextFieldRenderElement e = mount(tf);
        e.userEdited("hello");
        assertEquals(List.of("hello"), changed);
        assertEquals("hello", ctl.text(), "controller absorbed the user edit");
        assertEquals(1, notified[0], "controller listeners fired once");
    }

    @Test
    void controllerSetTextAndClearNotifyListeners() {
        TextEditingController ctl = new TextEditingController();
        final int[] notified = {0};
        ctl.addListener(() -> notified[0]++);
        ctl.setText("abc");
        assertEquals("abc", ctl.text());
        ctl.clear();
        assertEquals("", ctl.text());
        assertEquals(2, notified[0]);
    }

    // ------------------------------------------------------------------
    // SnackBar consumption
    // ------------------------------------------------------------------

    @Test
    void snackBarContentTextIsConsumedWithDefaultDuration() {
        SnackBar sb = new SnackBar();
        sb.content(new com.codename1.flutter.widgets.Text("Saved"));
        ScaffoldMessengerState state = ScaffoldMessenger.of(null);
        state.showSnackBar(sb);
        assertEquals("Saved", state.lastMessage());
        assertEquals(SnackBar.DEFAULT_DURATION_MS, state.lastDurationMillis());

        SnackBar timed = new SnackBar();
        timed.content(new com.codename1.flutter.widgets.Text("Bye"));
        timed.duration(dart.core.Duration.of(0, 0, 0, 2, 0, 0));
        state.showSnackBar(timed);
        assertEquals(2000, state.lastDurationMillis());
    }

    @Test
    void maxLengthLimitsWhatTheControllerAndOnChangedSee() {
        final List<String> changed = new ArrayList<String>();
        TextEditingController ctl = new TextEditingController();
        TextField tf = new TextField();
        tf.controller(ctl);
        tf.maxLength(4);
        tf.onChanged(changed::add);
        TextFieldRenderElement e = mount(tf);
        e.userEdited("abcdef");
        assertEquals("abcd", ctl.text(), "the controller never holds more than maxLength");
        assertEquals(List.of("abcd"), changed);
    }

    @Test
    void oneControllerDrivesEveryFieldItIsGivenTo() {
        TextEditingController ctl = new TextEditingController();
        TextField first = new TextField();
        first.controller(ctl);
        TextField second = new TextField();
        second.controller(ctl);
        TextFieldRenderElement a = mount(first);
        TextFieldRenderElement b = mount(second);
        a.userEdited("typed");
        assertEquals("typed", ctl.text());
        com.codename1.flutter.FlutterUI.unmountTree(b);
        ctl.setText("set");
        assertEquals("set", ctl.text(), "unmounting the second field must not orphan the first");
    }
}
