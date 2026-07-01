// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::native-themes-java-001[]
Hashtable override = new Hashtable();
override.put("@accent-color", "ff2d95");
override.put("@accent-color-dark", "ff2d95");
override.put("@accent-pressed-color", "c71a75");
override.put("@accent-pressed-color-dark", "c71a75");
override.put("@accent-on-color", "ffffff");
override.put("@accent-container-color", "ff2d95");
override.put("@accent-container-color-dark", "ff2d95");
override.put("@accent-on-container-color", "ffffff");
override.put("@accent-on-container-color-dark", "ffffff");
UIManager.getInstance().addThemeProps(override);
Form.getCurrentForm().refreshTheme();
// end::native-themes-java-001[]
