// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::commerce-java-001[]
if (CommerceManager.getInstance().isEntitled("pro")) {
    // unlock pro features
}
// end::commerce-java-001[]

// tag::commerce-java-002[]
CommerceManager cm = CommerceManager.getInstance();
cm.setAppUserId(myAccountId);   // optional; a stable id for the signed-in user
// end::commerce-java-002[]

// tag::commerce-java-003[]
cm.subscribe("pro_monthly");
// or cm.purchase("remove_ads");
// end::commerce-java-003[]

// tag::commerce-java-004[]
CN.callSerially(() -> {});                // (illustrative)
new Thread(() -> {
    cm.refresh();
    if (cm.isEntitled("pro")) { /* ... */ }
}).start();
// end::commerce-java-004[]
