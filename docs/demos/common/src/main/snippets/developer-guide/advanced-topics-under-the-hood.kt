// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::advanced-topics-under-the-hood-kotlin-001[]
package com.codename1.hellokotlin2

import com.codename1.ui.Button
import com.codename1.ui.Form
import com.codename1.ui.Label
import com.codename1.ui.layouts.BoxLayout

/**
 * Created by shannah on 2017-07-10.
 */
class KotlinForm : Form {

    constructor() : super("Hello Kotlin", BoxLayout.y()) {
        val label = Label("Hello Kotlin")
        val clickMe = Button("Click Me")
        clickMe.addActionListener {
            label.setText("You Clicked Me");
            revalidate();
        }

        add(label).add(clickMe);

    }


}
// end::advanced-topics-under-the-hood-kotlin-001[]

// tag::advanced-topics-under-the-hood-kotlin-002[]
package com.mycompany.myapp

class HelloKotlin {

    fun hello() {
        System.out.println("Hello from Kotlin");
    }
}
// end::advanced-topics-under-the-hood-kotlin-002[]
