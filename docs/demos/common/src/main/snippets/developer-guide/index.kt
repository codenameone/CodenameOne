// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::index-kotlin-001[]
class MyApplication {
    private var current: Form? = null
    private var theme: Resources? = null

    fun init(context: Any) {
        theme = UIManager.initFirstTheme("/theme")
        Toolbar.setGlobalToolbar(true)
        Log.bindCrashProtection(true)
    }

    fun start() {
        if (current != null) {
            current!!.show()
            return
        }
        val hi = Form("Hi World", BoxLayout.y())
        hi.add(Label("Hi World"))
        hi.show()
    }

    fun stop() {
        current = getCurrentForm()
        if (current is Dialog) {
            (current as Dialog).dispose()
            current = getCurrentForm()
        }
    }

    fun destroy() {
    }
}
// end::index-kotlin-001[]

// tag::index-kotlin-002[]
open class MyApplication
// end::index-kotlin-002[]

// tag::index-kotlin-003[]
open class MyApplication {
    private var current: Form? = null
    private var theme: Resources? = null
    fun init(context: Any?) {
        theme = UIManager.initFirstTheme("/theme")
        Toolbar.setGlobalToolbar(true)
        Log.bindCrashProtection(true)
    }

    fun start() {
        if (current != null) {
            current!!.show()
            return
        }
        val hi = Form("Hi World", BoxLayout.y())
        hi.add(Label("Hi World"))
        hi.show()
    }

    fun stop() {
        current = getCurrentForm()
        if (current is Dialog) {
            (current as Dialog).dispose()
            current = getCurrentForm()
        }
    }

    fun destroy() {
    }
}
// end::index-kotlin-003[]
