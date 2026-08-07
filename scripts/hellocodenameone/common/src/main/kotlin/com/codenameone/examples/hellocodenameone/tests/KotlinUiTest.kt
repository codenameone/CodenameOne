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
package com.codenameone.examples.hellocodenameone.tests

import com.codename1.components.Accordion
import com.codename1.components.MultiButton
import com.codename1.components.Switch
import com.codename1.ui.Button
import com.codename1.ui.CheckBox
import com.codename1.ui.Container
import com.codename1.ui.Label
import com.codename1.ui.Slider
import com.codename1.ui.TextArea
import com.codename1.ui.TextField
import com.codename1.ui.layouts.BoxLayout

class KotlinUiTest : BaseTest() {
    // Breadcrumbs because the Windows port reports NullPointerException with no
    // stack at all -- Display.getStackTrace and Throwable.getStackTrace both come
    // back empty there, so CI gives a bare exception and no location. Cheap on
    // every other port, and the difference between naming the failing
    // construction and guessing at it. Remove once the Windows failure is fixed.
    private fun step(name: String) {
        System.out.println("CN1SS:INFO:kotlin-step=" + name)
    }

    override fun runTest(): Boolean {
        step("form")
        val kotlinForm = createForm("Kotlin", BoxLayout.y(), "kotlin")
        step("label")
        val label = Label("Kotlin UI Test Components")
        step("button")
        val button = Button("Kotlin Button")
        step("switch-off")
        val switchOff = Switch()
        step("switch-on-ctor")
        val switchOn = Switch()
        step("switch-on-seton")
        switchOn.setOn()
        step("switch-row")
        val switchRow = BoxLayout.encloseX(switchOff, switchOn)
        step("textfield")
        val textField = TextField("", "Enter name")
        step("slider")
        val slider = Slider()
        slider.isEditable = true
        slider.progress = 50
        step("addAll")
        kotlinForm.addAll(label, button, switchRow, textField, slider)
        step("added")

        step("accordion-ctor")
        val accordion = Accordion()
        step("multibutton-1")
        val mb1 = MultiButton("MultiButton Line 1")
        mb1.setTextLine2("Additional detail line")
        step("multibutton-2")
        val mb2 = MultiButton("MultiButton Line 2")
        mb2.setTextLine2("More detail for Kotlin UI")
        step("accordion-details")
        accordion.addContent("Details", BoxLayout.encloseY(mb1, mb2))

        step("checkbox")
        val check = CheckBox("Enable notifications")
        step("prefs-switch")
        val prefSwitch = Switch()
        prefSwitch.setOn()
        step("textarea")
        val note = TextArea(3, 20)
        step("textarea-hint")
        note.hint = "Add a short note"
        step("prefs-container")
        val preferences = Container(BoxLayout.y())
        preferences.addAll(check, prefSwitch, note)
        // A component that paints a drop shadow blurs an image and then draws
        // ON TOP of the blur result -- Switch does exactly this for its ON thumb.
        // A port whose blur hands back something undrawable therefore cannot
        // render a Switch at all, and on Windows it did not: gaussianBlurImage
        // returned an ARGB-backed image with no render target, getGraphics wrapped
        // a null native peer, and the first call that read through it faulted. The
        // fault surfaced as a bare NullPointerException with no message and no
        // frames, because the port maps a null-address access violation to one.
        // Nothing in the suite covered "draw on a blur result", so this is the
        // check that would have caught it. Run on a throwaway Accordion that is
        // never added to the form: an earlier version probed the rendered one and
        // changed the kotlin golden on every other port.
        val probe = Accordion()
        step("probe-blur-drawable")
        if (com.codename1.ui.Display.getInstance().isGaussianBlurSupported) {
            val shadow = com.codename1.ui.ImageFactory.createImage(prefSwitch, 34, 34, 0)
            val sg = shadow.graphics
            sg.color = 0
            sg.fillRoundRect(2, 2, 30, 30, 30, 30)
            val blurred = com.codename1.ui.Display.getInstance().gaussianBlurImage(shadow, 5f)
                    ?: throw IllegalStateException("gaussianBlurImage returned null")
            // The calls Switch makes on the blur result, in its order. concatenateAlpha
            // reads the graphics state and is the first one to dereference the peer.
            val bg = blurred.graphics
            bg.concatenateAlpha(255)
            bg.color = 0xffffff
            bg.fillRoundRect(2, 2, 30, 30, 30, 30)
            step("probe-blur-drawable-ok=" + blurred.width + "x" + blurred.height)
        }
        // An ON switch inside a container is what first exposed the above: the OFF
        // thumb takes its shadow spread from switchThumbShadowSpreadInt (0 for the
        // flat Material 3 thumb) and so never blurs, while the ON thumb hard-codes
        // a spread of 2. Keep both shapes covered.
        step("probe-switch-on-preferred")
        val soSwitch = Switch()
        soSwitch.setOn()
        probe.addContent("ProbeSwitchOn", Container(BoxLayout.y()).apply { add(soSwitch) })
        soSwitch.preferredSize
        step("probe-switch-on-ok")

        step("accordion-prefs")
        accordion.addContent("Preferences", preferences)

        step("accordion-summary")
        accordion.addContent("Summary", BoxLayout.encloseY(
                Label("Accordion showcases grouped UI"),
                Button("Confirm Settings")
        ))

        step("form-add")
        kotlinForm.add(accordion)
        step("form-show")
        kotlinForm.show()
        step("shown")
        return true
    }
}