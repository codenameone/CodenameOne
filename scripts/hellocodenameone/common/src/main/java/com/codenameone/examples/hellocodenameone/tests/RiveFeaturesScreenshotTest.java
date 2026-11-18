/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.generated.rive.ComplexIkDependencyRive;
import com.codename1.generated.rive.DataBindingTest2Rive;
import com.codename1.generated.rive.HelloWorldRive;
import com.codename1.generated.rive.LayoutComplex1Rive;
import com.codename1.generated.rive.LightSwitchRive;
import com.codename1.generated.rive.StackedPathEffectsRive;
import com.codename1.generated.rive.StateMachineTriggersRive;
import com.codename1.generated.rive.TrimRive;
import com.codename1.rive.RiveFile;
import com.codename1.rive.RiveView;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;

/// One still frame per Rive feature family, each driven to a fixed point
/// through the public `RiveView` API and paused (views play by default), so the capture does
/// not depend on the clock:
///
/// - `light_switch`: a state machine boolean input set, then advanced;
/// - `state_machine_triggers`: a trigger fired, then advanced;
/// - `hello_world`: text with an embedded font;
/// - `layout_complex1`: nested layouts;
/// - `data_binding_test_2`: a view-model number bound into the artboard;
/// - `stacked_path_effects` and `trim`: trim and dash path effects;
/// - `complex_ik_dependency`: bones under an IK constraint.
///
/// None of the files has a mesh, so every view draws through `Graphics`;
/// `RiveGpuScreenshotTest` covers the GPU path.
public class RiveFeaturesScreenshotTest extends BaseTest {
    @Override
    public boolean runTest() {
        Form form = createForm("Rive features", new GridLayout(4, 2), "RiveFeatures");

        RiveView light = new RiveView(new LightSwitchRive());

        light.pause();
        light.setBoolean("On", true);
        light.advance(1f);
        form.add(cell("state machine bool", light));

        RiveView trigger = new RiveView(new StateMachineTriggersRive());

        trigger.pause();
        trigger.fireTrigger("Trigger 1");
        trigger.advance(0.5f);
        form.add(cell("trigger", trigger));

        form.add(cell("text", still(new HelloWorldRive(), 0f)));
        form.add(cell("layout", still(new LayoutComplex1Rive(), 0f)));

        RiveView bound = new RiveView(new DataBindingTest2Rive());

        bound.pause();
        bound.setViewModelNumber("num", 50f);
        bound.advance(0.5f);
        form.add(cell("data binding", bound));

        form.add(cell("path effects", still(new StackedPathEffectsRive(), 0.5f)));
        form.add(cell("trim", still(new TrimRive(), 0.5f)));
        form.add(cell("bones + IK", still(new ComplexIkDependencyRive(), 0.5f)));
        form.show();
        return true;
    }

    private static RiveView still(RiveFile file, float seconds) {
        RiveView v = new RiveView(file);
        v.pause();
        v.advance(seconds);
        return v;
    }

    private static Container cell(String name, RiveView view) {
        Container c = new Container(new BorderLayout());
        c.add(BorderLayout.CENTER, view);
        c.add(BorderLayout.SOUTH, new Label(name));
        return c;
    }
}
