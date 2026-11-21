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

/// One field of a back-office form ([FormView]). `type` is the input's
/// kind -- text, number, date, email, textarea, select, checkbox or hidden --
/// and the template draws each with its label.
public class FormField {
    public String name = "";
    public String label = "";
    public String type = "text";
    public String value = "";
    public String hint = "";
    public boolean checked;
    public boolean required;
    /// A select's choices: `id` the value, `title` the words, `flag` selected.
    public List<AdminRow> options = new ArrayList<AdminRow>();

    public FormField() {
    }

    public FormField(String name, String label, String type, String value) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.value = value == null ? "" : value;
    }

    /// Adds a choice to a select, selected when it is the field's value.
    public FormField option(String id, String title) {
        AdminRow r = new AdminRow();
        r.id = id;
        r.title = title;
        r.flag = id.equals(value);
        options.add(r);
        return this;
    }

    public FormField hint(String text) {
        hint = text;
        return this;
    }
}
