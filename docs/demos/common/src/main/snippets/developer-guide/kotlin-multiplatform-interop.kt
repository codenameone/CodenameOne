// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::kotlin-multiplatform-interop-kotlin-001[]
plugins {
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}
// end::kotlin-multiplatform-interop-kotlin-001[]

// tag::kotlin-multiplatform-interop-kotlin-002[]
class MyApp : Lifecycle() {
    override fun runApp() {
        ComposeForm { App() }.show()
    }
}

// Or beside other components:
val form = Form("Mixed", BorderLayout())
form.add(BorderLayout.NORTH, Label("Codename One"))
form.add(BorderLayout.CENTER, ComposeView { Schedule() })
form.show()
// end::kotlin-multiplatform-interop-kotlin-002[]
