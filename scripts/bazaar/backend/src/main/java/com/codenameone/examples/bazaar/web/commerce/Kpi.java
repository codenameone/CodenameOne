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
package com.codenameone.examples.bazaar.web.commerce;

/// A headline number on the dashboard, with its change against the period
/// before already worked out.
public class Kpi {
    public String label = "";
    public String value = "";
    public String change = "";
    /// "up", "down" or "".
    public String trend = "";

    public Kpi() {
    }

    public Kpi(String label, String value, long now, long before) {
        this.label = label;
        this.value = value;
        if (before > 0) {
            long pct = (now - before) * 100 / before;
            change = (pct > 0 ? "+" : "") + pct + "% on the 30 days before";
            trend = pct > 0 ? "up" : pct < 0 ? "down" : "";
        } else if (now > 0) {
            change = "None in the 30 days before";
            trend = "up";
        }
    }
}
