/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.web.admin;

import java.util.ArrayList;
import java.util.List;

/// A back-office form drawn by one template fragment: a card with a heading,
/// a line of explanation, its [FormField]s and a submit button. Every one
/// posts with the page's CSRF token and is answered with 303.
public class FormView {
    public String id = "";
    public String title = "";
    public String text = "";
    public String action = "";
    public String submit = "Save";
    /// A red button, and the question htmx asks before it posts.
    public boolean danger;
    public String confirm = "";
    public List<FormField> fields = new ArrayList<FormField>();

    public FormView() {
    }

    public FormView(String id, String title, String action, String submit) {
        this.id = id;
        this.title = title;
        this.action = action;
        this.submit = submit;
    }

    public FormField add(String name, String label, String type, String value) {
        FormField f = new FormField(name, label, type, value);
        fields.add(f);
        return f;
    }

    public FormField check(String name, String label, boolean on) {
        FormField f = new FormField(name, label, "checkbox", "1");
        f.checked = on;
        fields.add(f);
        return f;
    }
}
