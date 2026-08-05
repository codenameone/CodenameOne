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

        val accordion = Accordion()
        accordion.addContent("Details", BoxLayout.encloseY(
                MultiButton("MultiButton Line 1").apply {
                    setTextLine2("Additional detail line")
                },
                MultiButton("MultiButton Line 2").apply {
                    setTextLine2("More detail for Kotlin UI")
                }
        ))

        val preferences = Container(BoxLayout.y())
        preferences.addAll(
                CheckBox("Enable notifications"),
                Switch().apply { setOn() },
                TextArea(3, 20).apply { hint = "Add a short note" }
        )
        accordion.addContent("Preferences", preferences)

        accordion.addContent("Summary", BoxLayout.encloseY(
                Label("Accordion showcases grouped UI"),
                Button("Confirm Settings")
        ))

        kotlinForm.add(accordion)
        kotlinForm.show()
        return true
    }
}