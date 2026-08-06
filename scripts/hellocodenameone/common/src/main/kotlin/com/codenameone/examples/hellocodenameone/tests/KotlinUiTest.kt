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
        step("probe-checkbox")
        accordion.addContent("ProbeCheck", Container(BoxLayout.y()).apply { add(CheckBox("probe")) })
        step("probe-switch")
        accordion.addContent("ProbeSwitch", Container(BoxLayout.y()).apply { add(Switch()) })
        step("probe-textarea")
        accordion.addContent("ProbeText", Container(BoxLayout.y()).apply { add(TextArea(3, 20)) })
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