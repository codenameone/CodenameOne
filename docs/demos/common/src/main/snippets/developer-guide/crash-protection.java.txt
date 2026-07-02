// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::crash-protection-java-001[]
public void init(Object context) {
    CrashProtection.install();
    CrashProtection.setEnabled(true);  // default is false; user-controlled opt-in
}
// end::crash-protection-java-001[]
