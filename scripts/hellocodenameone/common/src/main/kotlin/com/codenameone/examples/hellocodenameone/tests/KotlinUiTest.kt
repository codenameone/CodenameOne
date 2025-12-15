package com.codenameone.examples.hellocodenameone.tests

import com.codename1.components.Accordion
import com.codename1.components.MultiButton
import com.codename1.components.Switch
import com.codename1.ui.Button
import com.codename1.ui.layouts.BoxLayout

class KotlinUiTest : BaseTest() {
    override fun runTest(): Boolean {
        var kotlinForm = createForm("Kotlin", BoxLayout.y(), "kotlin")
        kotlinForm.add(Button("Kotlin Button"))
        var on = Switch()
        on.setOn()
        kotlinForm.add(BoxLayout.encloseX(Switch(), on))
        var acc = Accordion()
        acc.add(MultiButton("MultiButton Line 1"))
        kotlinForm.add(acc);
        kotlinForm.show()
        return true
    }
}