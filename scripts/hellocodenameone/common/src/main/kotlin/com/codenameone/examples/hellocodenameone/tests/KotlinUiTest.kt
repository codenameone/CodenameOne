package com.codenameone.examples.hellocodenameone.tests

import com.codename1.components.Accordion
import com.codename1.components.MultiButton
import com.codename1.components.Switch
import com.codename1.ui.Button
import com.codename1.ui.CheckBox
import com.codename1.ui.Container
import com.codename1.ui.Label
import com.codename1.ui.Sheet
import com.codename1.ui.Slider
import com.codename1.ui.TextArea
import com.codename1.ui.TextField
import com.codename1.ui.layouts.BoxLayout
import com.codename1.ui.util.UITimer

class KotlinUiTest : BaseTest() {
    override fun runTest(): Boolean {
        val kotlinForm = createForm("Kotlin", BoxLayout.y(), "kotlin")
        kotlinForm.addAll(
                Label("Kotlin UI Test Components"),
                Button("Kotlin Button"),
                BoxLayout.encloseX(Switch(), Switch().apply { setOn() }),
                TextField("", "Enter name"),
                Slider().apply {
                    isEditable = true
                    progress = 50
                }
        )

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

        val sheet = Sheet(null, "Overlay Sheet")
        sheet.contentPane.add(Label("This is a sheet covering part of the screen"))
        sheet.show(0)

        return true
    }
}