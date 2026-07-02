// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::svg-transcoder-java-001[]
Image home = new com.codename1.generated.svg.Home(6f, 6f);  // 6mm × 6mm
button.setIcon(home);
// end::svg-transcoder-java-001[]

// tag::svg-transcoder-java-002[]
public Home(float widthMm, float heightMm) { }   // for cn1-svg-width / cn1-svg-height
public Home(int sourceDensity) { }               // for cn1-source-dpi hints
public Home() { }                                // default DENSITY_MEDIUM source
// end::svg-transcoder-java-002[]

// tag::svg-transcoder-java-003[]
Image spin = Resources.getGlobalResources().getImage("spinner.json");
// or by stem, like a multi-image:
Image spin2 = Resources.getGlobalResources().getImage("spinner");
// end::svg-transcoder-java-003[]
