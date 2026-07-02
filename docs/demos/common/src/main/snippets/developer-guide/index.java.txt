// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::index-java-001[]
public class HelloWorld { // <1>
    private Form current; // <2>
    private Resources theme; // <3>

    // ... class methods ...
}
// end::index-java-001[]

// tag::index-java-002[]
public void init(Object context) { // <1>
    updateNetworkThreadCount(2); // <2>
    theme = UIManager.initFirstTheme("/theme"); // <3>
    Toolbar.setGlobalToolbar(true); // <4>
    Log.bindCrashProtection(true); // <5>
    addNetworkErrorListener(err -> { // <6>
        err.consume(); // <7>
        if(err.getError() != null) { // <8>
            Log.e(err.getError());
        }
        Log.sendLogAsync(); // <9>
        Dialog.show("Connection Error", // <10>
            "There was a networking error in the connection to " +
            err.getConnectionRequest().getUrl(), "OK", null);
    });
}
// end::index-java-002[]

// tag::index-java-003[]
public void start() {
    if(current != null){ // <1>
        current.show(); // <2>
        return;
    }
    Form hi = new Form("Hi World", BoxLayout.y()); // <3>
    hi.add(new Label("Hi World")); // <4>
    hi.show(); // <5>
}
// end::index-java-003[]

// tag::index-java-004[]
public void stop() { // <1>
    current = getCurrentForm(); // <2>
    if(current instanceof Dialog) { // <3>
        ((Dialog)current).dispose();
        current = getCurrentForm();
    }
}

public void destroy() { // <4>
}
// end::index-java-004[]

// tag::index-java-005[]
public class MyApplication {
    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);
    }

    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form hi = new Form("Hi World", BoxLayout.y());
        hi.add(new Label("Hi World"));
        hi.show();
    }

    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }

    public void destroy() {
    }
}
// end::index-java-005[]
