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

import com.codenameone.examples.bazaar.content.RichText;
import java.util.ArrayList;
import java.util.List;

/// A page, policy or blog post as the storefront draws it. The body is the
/// [RichText] block model, printed by the template as text in fixed
/// elements: no markup from the body reaches the page.
public final class ContentView {
    public String title = "";
    public boolean post;
    public String author = "";
    public String date = "";
    public final List<LinkView> tags = new ArrayList<LinkView>();
    public final List<Block> blocks = new ArrayList<Block>();
    /// The blog index, for a post.
    public String blogUrl = "";

    /// A run of text: bold, italic or a link.
    public static final class Run {
        public String text = "";
        public String href = "";
        public String css = "";
        public boolean link;
        public boolean plain;
        public boolean br;
    }

    /// A list item.
    public static final class Item {
        public final List<Run> runs = new ArrayList<Run>();
    }

    /// A paragraph, heading, quote or list.
    public static final class Block {
        public boolean p;
        public boolean h2;
        public boolean h3;
        public boolean quote;
        public boolean ul;
        public boolean ol;
        public final List<Run> runs = new ArrayList<Run>();
        public final List<Item> items = new ArrayList<Item>();
    }

    /// The blocks of a stored (already clean) body.
    public static List<Block> blocks(String html) {
        List<RichText.Block> in = RichText.parse(html);
        List<Block> out = new ArrayList<Block>();
        for (int b = 0; b < in.size(); b++) {
            RichText.Block src = in.get(b);
            Block dst = new Block();
            dst.p = src.p;
            dst.h2 = src.h2;
            dst.h3 = src.h3;
            dst.quote = src.quote;
            dst.ul = src.ul;
            dst.ol = src.ol;
            runs(src.runs, dst.runs);
            for (int i = 0; i < src.items.size(); i++) {
                Item item = new Item();
                runs(src.items.get(i).runs, item.runs);
                dst.items.add(item);
            }
            out.add(dst);
        }
        return out;
    }

    private static void runs(List<RichText.Run> in, List<Run> out) {
        for (int iter = 0; iter < in.size(); iter++) {
            RichText.Run src = in.get(iter);
            Run r = new Run();
            r.text = src.text;
            r.href = src.href;
            r.css = src.css;
            r.link = src.link;
            r.plain = src.plain;
            r.br = src.br;
            out.add(r);
        }
    }
}
