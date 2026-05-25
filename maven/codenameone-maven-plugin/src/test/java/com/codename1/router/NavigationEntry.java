/*
 * Test stub of com.codename1.router.NavigationEntry.
 */
package com.codename1.router;

import com.codename1.ui.Form;

public final class NavigationEntry {
    private final String path;
    private final Form form;

    NavigationEntry(String path, Form form) {
        this.path = path;
        this.form = form;
    }

    public String getPath() { return path; }
    public Form getForm() { return form; }
    public String getTitle() {
        String t = form == null ? null : form.getTitle();
        return t == null ? "" : t;
    }
}
