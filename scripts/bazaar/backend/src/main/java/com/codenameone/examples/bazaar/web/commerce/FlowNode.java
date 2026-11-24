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

/// A step of a journey flowchart, placed by the server: a box with the
/// step's name, a line of detail and its counts, and its ports.
public class FlowNode {
    public String id = "";
    public String title = "";
    public String detail = "";
    public String counts = "";
    /// "entry", "step", "split" or "end", for the colour.
    public String kind = "";
    public int x;
    public int y;
    public int width;
    public int height;
    public int textX;
    public int titleY;
    public int detailY;
    public int countsY;
    /// The input port (left middle) and the output port (right middle).
    public int inX;
    public int outX;
    public int portY;
    public boolean hasIn;
    public boolean hasOut;
}
