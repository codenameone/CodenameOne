// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::wearables-java-001[]
Form f = new Form(BoxLayout.y());
if (CN.isWatch()) {
    // Compact, single-column layout suited to a small round/square screen
    f.add(new Label("Hi Watch"));
    f.getToolbar().setVisible(false);
} else {
    // Full phone/tablet layout
    f.add(new SpanLabel("Welcome to the full size application"));
}
f.show();
// end::wearables-java-001[]
