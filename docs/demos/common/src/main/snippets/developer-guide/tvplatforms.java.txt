// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::tvplatforms-java-001[]
Form f = new Form(BoxLayout.y());
if (CN.isTV()) {
    // 10-foot UI: larger fonts, generous spacing, focus-driven navigation
    f.add(new Label("Hello TV"));
} else {
    // Full phone/tablet layout
    f.add(new Label("Hello"));
}
f.show();
// end::tvplatforms-java-001[]
