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

import com.codename1.components.ScaleImageLabel;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.Resources;

/// One still frame per Rive feature family, each a `.riv` image driven to a
/// fixed point through the interactive-animation API of `Image` and paused
/// (animations play by default), so the capture does not depend on the clock:
///
/// - `light_switch`: a state machine boolean input set, then advanced;
/// - `state_machine_triggers`: a trigger fired, then advanced;
/// - `hello_world`: text with an embedded font;
/// - `layout_complex1`: nested layouts;
/// - `data_binding_test_2`: a view-model number bound into the artboard;
/// - `stacked_path_effects` and `trim`: trim and dash path effects;
/// - `complex_ik_dependency`: bones under an IK constraint.
public class RiveFeaturesScreenshotTest extends BaseTest {
    @Override
    public boolean runTest() {
        Form form = createForm("Rive features", new GridLayout(4, 2), "RiveFeatures");
        // Not scrollable: the grid then divides the screen into eight cells.
        form.setScrollable(false);

        Image light = fresh("light_switch.riv");
        light.setAnimationInput("On", true);
        RiveAnimatedScreenshotTest.step(light, 1f);
        form.add(cell("state machine bool", light));

        Image trigger = fresh("state_machine_triggers.riv");
        trigger.fireAnimationTrigger("Trigger 1");
        RiveAnimatedScreenshotTest.step(trigger, 0.5f);
        form.add(cell("trigger", trigger));

        form.add(cell("text", still("hello_world.riv", 0f)));
        form.add(cell("layout", still("layout_complex1.riv", 0f)));

        Image bound = fresh("data_binding_test_2.riv");
        bound.setAnimationProperty("num", Float.valueOf(50f));
        RiveAnimatedScreenshotTest.step(bound, 0.5f);
        form.add(cell("data binding", bound));

        form.add(cell("path effects", still("stacked_path_effects.riv", 0.5f)));
        form.add(cell("trim", still("trim.riv", 0.5f)));
        form.add(cell("bones + IK", still("complex_ik_dependency.riv", 0.5f)));
        form.show();
        return true;
    }

    /// A paused image of `name` in its initial state, independent of the
    /// shared one the resources hold.
    static Image fresh(String name) {
        Image shared = Resources.getGlobalResources().getImage(name);
        Image img = shared.scaled(shared.getWidth(), shared.getHeight());
        img.setAnimationPlaying(false);
        return img;
    }

    private static Image still(String name, float seconds) {
        Image img = fresh(name);
        RiveAnimatedScreenshotTest.step(img, seconds);
        return img;
    }

    private static Container cell(String name, Image img) {
        Container c = new Container(new BorderLayout());
        c.add(BorderLayout.CENTER, holder(img));
        c.add(BorderLayout.SOUTH, new Label(name));
        return c;
    }

    /// The image stretched over a label that fills the inner area of a
    /// `RiveView`-styled container; the image fits itself into that area,
    /// keeping its aspect ratio.
    static Container holder(Image img) {
        Container holder = new Container(new BorderLayout(), "RiveView");
        ScaleImageLabel label = new ScaleImageLabel(img);
        Style s = label.getAllStyles();
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
        s.setBorder(Border.createEmpty());
        s.setBgTransparency(0);
        label.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED);
        holder.add(BorderLayout.CENTER, label);
        return holder;
    }
}
