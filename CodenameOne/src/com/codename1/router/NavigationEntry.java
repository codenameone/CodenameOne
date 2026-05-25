/// A single frame on the `Navigation` stack: the URL that produced the form
/// and the `Form` instance the route built. Returned from
/// `Navigation#getStack`, `Navigation#getCurrent`, and accepted by
/// `Navigation#popTo` so a breadcrumb UI can pop back to any prior entry.
///
/// Entries are immutable value objects; equality is by identity.
package com.codename1.router;

import com.codename1.ui.Form;

public final class NavigationEntry {

    private final String path;
    private final Form form;

    NavigationEntry(String path, Form form) {
        this.path = path;
        this.form = form;
    }

    /// The path (URL minus scheme + host) that produced this entry, e.g.
    /// `/users/42`.
    public String getPath() {
        return path;
    }

    /// The `Form` instance the route builder produced.
    public Form getForm() {
        return form;
    }

    /// Convenience: the form's title, useful as a breadcrumb label. Returns
    /// the empty string when the form has no title set.
    public String getTitle() {
        String t = form == null ? null : form.getTitle();
        return t == null ? "" : t;
    }

    @Override
    public String toString() {
        return "NavigationEntry{" + path + "}";
    }
}
