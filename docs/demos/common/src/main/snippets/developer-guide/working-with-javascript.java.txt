// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::working-with-javascript-java-001[]
import com.codename1.io.Log;
class Class1 {
    public static int getValue() {
        Log.p("Hello world");
        return 1;
    }
}
// end::working-with-javascript-java-001[]

// tag::working-with-javascript-java-002[]
class Class2 {
    public static int value = Class1.getValue();

}
// end::working-with-javascript-java-002[]

// tag::working-with-javascript-java-003[]
    public static int getValue() {
        return 1;
    }
// end::working-with-javascript-java-003[]

// tag::working-with-javascript-java-004[]
Display.getInstance().setProperty("javascript.useProxyForSameDomain", "true");
// end::working-with-javascript-java-004[]

// tag::working-with-javascript-java-005[]
Display d = Display.getInstance();
if (d.getProperty("User-Agent", "Unknown").indexOf("Android") != -1) {
    d.setProperty("javascript.native.theme", "/androidTheme.res");
}
// end::working-with-javascript-java-005[]

// tag::working-with-javascript-java-006[]
Form f = new Form("Test Before Unload", BoxLayout.y());
CheckBox enableBeforeUnload = new CheckBox("Enable Before Unload");
enableBeforeUnload.setSelected(true);
enableBeforeUnload.addActionListener(e->{
    if (enableBeforeUnload.isSelected()) {
        CN.setProperty("platformHint.javascript.beforeUnloadMessage", "Are you sure you want to leave this page?  It might be bad");
    } else {
        CN.setProperty("platformHint.javascript.beforeUnloadMessage", null);
    }
});
f.add(enableBeforeUnload);
f.show();
// end::working-with-javascript-java-006[]

// tag::working-with-javascript-java-007[]
CN.setProperty("platformHint.javascript.backsideHooksInterval", "1000");

// Now your app will process media.play() and Display.execute(...) calls
// once per second (1000ms). If play() or execute() has been called anytime
// in that second (since the last poll), it will seamlessly process the
// request.


// To disable polling, just set it to an interval 0 or lower.
// for example, CN.setProperty("platformHint.javascript.backsideHooksInterval", "0");
// end::working-with-javascript-java-007[]

// tag::working-with-javascript-java-008[]
BrowserComponent browser = new BrowserComponent();
browser.putClientProperty("HTML5Peer.removeOnDeinitialize", Boolean.FALSE);
// end::working-with-javascript-java-008[]
