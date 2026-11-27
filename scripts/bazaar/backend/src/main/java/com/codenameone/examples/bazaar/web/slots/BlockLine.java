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

import com.codenameone.examples.bazaar.ops.Blocks;

/// One line of an app's storefront block as a page prints it: exactly one
/// of the kind flags is set, `text` is printed escaped, and `url` is an
/// https address the upload was checked for ([Blocks#isHttps]).
public class BlockLine {
    public String text = "";
    public String url = "";
    public String css = "app-line";
    public boolean heading;
    public boolean para;
    public boolean badge;
    public boolean link;
    public boolean image;
    public boolean item;
    public boolean divider;

    static BlockLine of(Blocks.Row row) {
        BlockLine l = new BlockLine();
        l.text = row.text;
        l.url = row.url;
        String k = row.kind;
        l.heading = Blocks.HEADING.equals(k);
        l.badge = Blocks.BADGE.equals(k);
        l.link = "link".equals(k);
        l.image = Blocks.IMAGE.equals(k);
        l.item = "item".equals(k);
        l.divider = Blocks.DIVIDER.equals(k);
        l.para = !l.heading && !l.badge && !l.link && !l.image && !l.item && !l.divider;
        StringBuilder css = new StringBuilder(l.item ? "app-line item" : "app-line");
        if (row.tone.length() > 0) {
            css.append(" tone-").append(row.tone);
        }
        if (row.depth > 1) {
            css.append(" depth-").append(Math.min(row.depth, 6));
        }
        l.css = css.toString();
        return l;
    }
}
