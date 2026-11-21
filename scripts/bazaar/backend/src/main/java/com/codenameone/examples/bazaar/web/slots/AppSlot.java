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

/// One app slot as it is stored: what an owner registered at a named place.
///
/// `scriptUrl` and `widgetHtml` are each optional, but one of them is always
/// set. `origins` is a space-separated list of `https://host[:port]` values
/// that passed [com.codenameone.examples.bazaar.web.security.CspSources#clean].
public class AppSlot {
    public String id = "";
    public String storeId = "";
    public String slot = "";
    public String name = "";
    public String scriptUrl = "";
    public String widgetHtml = "";
    public String origins = "";
    public String category = "";
    public boolean enabled;
    public long createdAt;
    public long updatedAt;
}
