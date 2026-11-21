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

/// One item of a list whose every entry carries its own buttons -- a
/// subscription with skip, pause and cancel, a quote with accept and
/// decline, an invoice with mark paid. Each button is a one-field form post
/// (`buttons`: `title` the label, `action` the address, `flag` a red one).
public class ActionCard {
    public String title = "";
    public String detail = "";
    public String meta = "";
    public String status = "";
    public String amount = "";
    /// A link on the title, or empty.
    public String url = "";
    /// A second link (a PDF), with its label.
    public String extraUrl = "";
    public String extraLabel = "";
    public List<AdminRow> buttons = new ArrayList<AdminRow>();

    public ActionCard button(String label, String action, boolean danger) {
        AdminRow r = new AdminRow();
        r.title = label;
        r.action = action;
        r.flag = danger;
        buttons.add(r);
        return this;
    }
}
