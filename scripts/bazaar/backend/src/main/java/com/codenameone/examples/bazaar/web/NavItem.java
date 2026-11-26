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
package com.codenameone.examples.bazaar.web;

/// A link in the sidebar, the store switcher or a tab strip.
public class NavItem {
    public String label = "";
    public String url = "";
    /// "page" for the current one, "false" otherwise (the `aria-current` value).
    public String current = "false";
    public boolean active;
    /// The `g`-prefixed shortcut letter shown beside it ("o" for g o), or empty.
    public String key = "";

    public NavItem() {
    }

    public NavItem(String label, String url, String key) {
        this.label = label;
        this.url = url;
        this.key = key == null ? "" : key;
    }

    public NavItem activeIf(String path) {
        active = path != null && (path.equals(url) || path.startsWith(url + "/")
                || path.startsWith(url + "?"));
        current = active ? "page" : "false";
        return this;
    }
}
