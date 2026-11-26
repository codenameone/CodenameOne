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
package com.codenameone.examples.bazaar.catalog;

import com.codenameone.examples.bazaar.api.NameCountDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The standard product categories: the first two levels of the Google
/// product taxonomy, read once from `taxonomy/google-product-taxonomy.txt`.
///
/// A product keeps a category id (0 for none). The id is what the shopping
/// feed sends as `google_product_category`, the path ("Apparel & Accessories >
/// Shoes") is what people pick from and what JSON-LD carries, and the top
/// level groups products for the storefront's category filter -- a filter on
/// a top-level category takes the categories under it too.
public final class Taxonomy {
    private static final String RESOURCE = "/taxonomy/google-product-taxonomy.txt";

    private static List<int[]> order;
    private static Map<Integer, String> paths;
    private static Map<Integer, Integer> parents;

    private Taxonomy() {
    }

    /// Every category, in the taxonomy's order, as id and path.
    public static List<NameCountDto> all() {
        load();
        List<NameCountDto> out = new ArrayList<NameCountDto>();
        for (int iter = 0; iter < order.size(); iter++) {
            int id = order.get(iter)[0];
            out.add(new NameCountDto(paths.get(Integer.valueOf(id)), id));
        }
        return out;
    }

    /// Whether `id` is a category (0, no category, is not).
    public static boolean known(int id) {
        load();
        return paths.containsKey(Integer.valueOf(id));
    }

    /// The path of a category, or empty for 0 or an unknown id.
    public static String path(int id) {
        load();
        String p = paths.get(Integer.valueOf(id));
        return p == null ? "" : p;
    }

    /// The last level of a category's path ("Shoes"), or empty.
    public static String name(int id) {
        String p = path(id);
        int at = p.lastIndexOf(" > ");
        return at < 0 ? p : p.substring(at + 3);
    }

    /// The top-level category of `id` (itself when it is one), or 0.
    public static int top(int id) {
        load();
        Integer parent = parents.get(Integer.valueOf(id));
        if (parent != null) {
            return parent.intValue();
        }
        return known(id) ? id : 0;
    }

    /// Whether a product in `categoryId` belongs under the filter `filter`:
    /// the same category, or one under it.
    public static boolean within(int categoryId, int filter) {
        return filter > 0 && categoryId > 0 && (categoryId == filter || top(categoryId) == filter);
    }

    /// The id whose path is `text` (ignoring ASCII case), or a number that is a
    /// known id; 0 for an empty text and -1 for anything else.
    public static int parse(String text) {
        String t = text == null ? "" : text.trim();
        if (t.length() == 0) {
            return 0;
        }
        load();
        boolean digits = t.length() < 10;
        for (int iter = 0; iter < t.length() && digits; iter++) {
            digits = t.charAt(iter) >= '0' && t.charAt(iter) <= '9';
        }
        if (digits) {
            int id = Integer.parseInt(t);
            return known(id) ? id : -1;
        }
        for (int iter = 0; iter < order.size(); iter++) {
            int id = order.get(iter)[0];
            if (paths.get(Integer.valueOf(id)).equalsIgnoreCase(t)) {
                return id;
            }
        }
        return -1;
    }

    /// Up to `limit` categories whose path contains every word of `query`
    /// (ignoring ASCII case), in the taxonomy's order; all of them, up to the
    /// limit, for an empty query.
    public static List<NameCountDto> search(String query, int limit) {
        load();
        List<String> words = new ArrayList<String>();
        String q = query == null ? "" : query.trim();
        int start = -1;
        for (int iter = 0; iter <= q.length(); iter++) {
            boolean space = iter == q.length() || q.charAt(iter) == ' ';
            if (space && start >= 0) {
                words.add(q.substring(start, iter));
                start = -1;
            } else if (!space && start < 0) {
                start = iter;
            }
        }
        List<NameCountDto> out = new ArrayList<NameCountDto>();
        for (int iter = 0; iter < order.size() && out.size() < limit; iter++) {
            int id = order.get(iter)[0];
            String p = paths.get(Integer.valueOf(id));
            boolean all = true;
            for (int w = 0; w < words.size() && all; w++) {
                all = Search.contains(p, words.get(w));
            }
            if (all) {
                out.add(new NameCountDto(p, id));
            }
        }
        return out;
    }

    private static synchronized void load() {
        if (paths != null) {
            return;
        }
        List<int[]> o = new ArrayList<int[]>();
        Map<Integer, String> p = new HashMap<Integer, String>();
        Map<Integer, Integer> up = new HashMap<Integer, Integer>();
        Map<String, Integer> tops = new HashMap<String, Integer>();
        String text = read();
        int lineStart = 0;
        while (lineStart < text.length()) {
            int end = text.indexOf('\n', lineStart);
            if (end < 0) {
                end = text.length();
            }
            String line = text.substring(lineStart, end).trim();
            lineStart = end + 1;
            int dash = line.indexOf(" - ");
            if (line.length() == 0 || line.charAt(0) == '#' || dash <= 0) {
                continue;
            }
            int id = Integer.parseInt(line.substring(0, dash));
            String path = line.substring(dash + 3);
            o.add(new int[] {id});
            p.put(Integer.valueOf(id), path);
            int gt = path.indexOf(" > ");
            if (gt < 0) {
                tops.put(path, Integer.valueOf(id));
            } else {
                Integer parent = tops.get(path.substring(0, gt));
                if (parent != null) {
                    up.put(Integer.valueOf(id), parent);
                }
            }
        }
        order = o;
        parents = up;
        paths = p;
    }

    private static String read() {
        try (InputStream in = Taxonomy.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return "";
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), "UTF-8");
        } catch (IOException unreadable) {
            return "";
        }
    }
}
