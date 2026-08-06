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
        // Bisect: addContent("Preferences", ...) is where the Windows port throws.
        // The container and all three children construct fine (the steps above all
        // print), so the failure is in what addContent does to one of them --
        // setHidden(true), which caches margins and forces a zero preferred size.
        // One probe section per child says which.
        // Probed on a throwaway Accordion that is never added to the form. The
        // first version of this put the probes into the real one, which changed
        // the rendered "kotlin" screenshot and failed the golden comparison on
        // every other port -- a diagnostic is not worth breaking six jobs for.
        // addContent does its work in the AccordionContent constructor, so an
        // unparented Accordion exercises exactly the same path and renders nothing.
        val probe = Accordion()
        step("probe-checkbox")
        probe.addContent("ProbeCheck", Container(BoxLayout.y()).apply { add(CheckBox("probe")) })
        step("probe-switch")
        probe.addContent("ProbeSwitch", Container(BoxLayout.y()).apply { add(Switch()) })
        step("probe-textarea")
        probe.addContent("ProbeText", Container(BoxLayout.y()).apply { add(TextArea(3, 20)) })
        // Round one: all three plain children passed, so the difference is what the
        // real container does beyond constructing them -- setOn() on the Switch, a
        // hint on the TextArea, or simply holding three children at once.
        // Split to the statement, because the composite probe cannot distinguish
        // constructing an on-switch from adding one to an Accordion -- and the
        // real test already shows Switch()+setOn() on its own is fine.
        step("probe-so-ctor")
        val soSwitch = Switch()
        step("probe-so-seton")
        soSwitch.setOn()
        step("probe-so-container")
        val soContainer = Container(BoxLayout.y())
        soContainer.add(soSwitch)
        // addContent on a container holding an ON switch is the failing statement.
        // The only state the ON path reaches that the OFF path does not is
        // getSelectedStyle() (getThumbOnImage uses it; getThumbOffImage uses the
        // unselected style), and calcPreferredSize is what asks for the thumb.
        // Both are public, so the accessor that throws can be named from here
        // without probes in Switch itself.
        step("probe-so-unselectedstyle")
        soSwitch.unselectedStyle
        step("probe-so-selectedstyle")
        soSwitch.selectedStyle
        // The track image is sized from the font height, and a zero height made
        // createMutableImage hand back a broken peer. Report the number.
        val f = soSwitch.style.font
        step("probe-so-fontheight=" + (if (f == null) "null-font" else f.height.toString()))
        step("probe-so-preferredsize")
        soSwitch.preferredSize
        step("probe-so-addcontent")
        probe.addContent("ProbeSwitchOn", soContainer)
        step("probe-so-done")
        step("probe-textarea-hint")
        probe.addContent("ProbeTextHint", Container(BoxLayout.y()).apply {
            add(TextArea(3, 20).apply { hint = "probe hint" })
        })
        step("probe-three-plain")
        probe.addContent("ProbeThree", Container(BoxLayout.y()).apply {
            add(CheckBox("a")); add(Switch()); add(TextArea(3, 20))
        })
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