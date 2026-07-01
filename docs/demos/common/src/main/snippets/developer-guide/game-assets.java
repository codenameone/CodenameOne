// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::game-assets-java-001[]
MaterialRegistry.register(new Material("lava", "Lava", 0xc0392b).setSolid(true));
Material m = MaterialRegistry.get("lava");   // never null; unknown ids return gray
// end::game-assets-java-001[]
