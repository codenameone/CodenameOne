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
package com.codenameone.examples.bazaar.web.slots;

/// One app slot snippet as a page prints it. What a kind becomes:
///
/// - `script`: a hidden element whose `data-src` the page script loads, since a
///   compiled view never prints a dynamic `<script src>`; the host is in the
///   page's `script-src`.
/// - `pixel`: a 1x1 image.
/// - `iframe` and `html`: a sandboxed frame (`allow-scripts` only, so it runs
///   in an opaque origin with no access to the page, its cookies or its
///   storage); `html` is served by this server at `/w/{id}`.
public class SlotView {
    public String id = "";
    public String kind = "";
    public String src = "";
    public String title = "";
    public String height = "120";
    public boolean script;
    public boolean pixel;
    public boolean frame;
}
