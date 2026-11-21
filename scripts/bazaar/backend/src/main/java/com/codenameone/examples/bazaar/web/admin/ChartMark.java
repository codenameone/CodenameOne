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

/// One mark of a server-drawn chart: a bar of the top-products chart, or a
/// labelled tick under the sales line. Coordinates are whole units of the
/// chart's `viewBox`, worked out in [AdminCharts].
public class ChartMark {
    public int x;
    public int y;
    public int width;
    public int height;
    /// The text beside the mark, and where it is drawn.
    public String label = "";
    public String value = "";
    public int textX;
    public int textY;
    public int valueX;
    /// A second line of text, and the fill opacity of a heat-map cell (a
    /// presentation attribute, so no inline style is needed under CSP).
    public String detail = "";
    public String opacity = "1";
}
