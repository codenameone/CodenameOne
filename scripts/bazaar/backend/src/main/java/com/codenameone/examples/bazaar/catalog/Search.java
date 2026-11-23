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
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Variant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// What a product search asks for, parsed from the query string, and the
/// matching itself. Everything is optional; text compares ignoring ASCII case.
final class Search {
    static final String COLLECTION = "collection";
    static final String TAG = "tag";
    static final String TYPE = "type";
    static final String VENDOR = "vendor";
    static final String PRICE = "price";
    static final String CATEGORY = "category";

    String[] words = new String[0];
    String collectionId = "";
    String tag = "";
    String type = "";
    String vendor = "";
    /// A taxonomy category, 0 for any; a top-level one takes those under it.
    int category;
    long min = -1;
    long max = -1;
    String sort = "relevance";
    int page;
    int size = 24;
    /// The shopper's content locale; null asks the request
    /// (`ProductText#shopperLocale`), empty means the originals.
    String locale;
    /// Product id to its title in [#locale], for the products translated;
    /// a word matches either title, and scores by the better one.
    Map<String, String> titles;

    /// A `category` parameter as a known taxonomy id, 0 (any) for anything else.
    static int category(String param) {
        long id = number(param, 0);
        return id > 0 && id <= Integer.MAX_VALUE && Taxonomy.known((int) id) ? (int) id : 0;
    }

    static Search of(String q, String tag, String type, String vendor, String min, String max,
            String sort, String page, String size) {
        Search search = new Search();
        search.words = words(q);
        search.tag = text(tag);
        search.type = text(type);
        search.vendor = text(vendor);
        search.min = number(min, -1);
        search.max = number(max, -1);
        String s = text(sort);
        search.sort = s.length() == 0 ? "relevance" : s;
        search.page = (int) Math.max(0, Math.min(10000, number(page, 0)));
        long n = number(size, 24);
        search.size = (int) (n < 1 ? 24 : Math.min(100, n));
        return search;
    }

    /// Whether a product matches every filter but `skip` (a facet's own,
    /// or null for none).
    boolean matches(Product product, List<String> tags, long minPrice, long maxPrice,
            Map<String, List<String>> membership, String skip) {
        for (int iter = 0; iter < words.length; iter++) {
            String[] alts = alternatives(words[iter]);
            boolean hit = false;
            for (int a = 0; a < alts.length && !hit; a++) {
                String word = alts[a];
                hit = contains(product.getTitle(), word) || contains(product.getVendor(), word)
                        || contains(product.getProductType(), word)
                        || contains(translatedTitle(product), word);
                for (int t = 0; t < tags.size() && !hit; t++) {
                    hit = contains(tags.get(t), word);
                }
            }
            if (!hit) {
                return false;
            }
        }
        if (!COLLECTION.equals(skip) && collectionId.length() > 0) {
            List<String> in = membership.get(product.getId());
            if (in == null || !in.contains(collectionId)) {
                return false;
            }
        }
        if (!TAG.equals(skip) && tag.length() > 0) {
            boolean hit = false;
            for (int t = 0; t < tags.size() && !hit; t++) {
                hit = tags.get(t).equalsIgnoreCase(tag);
            }
            if (!hit) {
                return false;
            }
        }
        if (!TYPE.equals(skip) && type.length() > 0
                && !type.equalsIgnoreCase(product.getProductType())) {
            return false;
        }
        if (!VENDOR.equals(skip) && vendor.length() > 0
                && !vendor.equalsIgnoreCase(product.getVendor())) {
            return false;
        }
        if (!CATEGORY.equals(skip) && category > 0
                && !Taxonomy.within(product.getCategoryId(), category)) {
            return false;
        }
        if (!PRICE.equals(skip)) {
            if (min >= 0 && maxPrice < min) {
                return false;
            }
            if (max >= 0 && minPrice > max) {
                return false;
            }
        }
        return true;
    }

    /// How well a product matches the words: a word in the title counts most.
    int score(Product product) {
        // The merchant's boost counts only when there is something to rank.
        int score = words.length == 0 ? 0 : product.getSearchBoost();
        String shown = translatedTitle(product);
        for (int iter = 0; iter < words.length; iter++) {
            if (startsWith(product.getTitle(), words[iter]) || startsWith(shown, words[iter])) {
                score += 3;
            } else if (contains(product.getTitle(), words[iter]) || contains(shown, words[iter])) {
                score += 2;
            } else {
                score++;
            }
        }
        return score;
    }

    static void count(Map<String, Integer> counts, List<String> order, String name) {
        if (name == null || name.length() == 0) {
            return;
        }
        Integer seen = counts.get(name);
        if (seen == null) {
            order.add(name);
            counts.put(name, Integer.valueOf(1));
        } else {
            counts.put(name, Integer.valueOf(seen.intValue() + 1));
        }
    }

    /// Counts as a list, the most common first, ties by name.
    static List<NameCountDto> list(Map<String, Integer> counts, List<String> order) {
        List<NameCountDto> out = new ArrayList<NameCountDto>();
        for (int iter = 0; iter < order.size(); iter++) {
            out.add(new NameCountDto(order.get(iter), counts.get(order.get(iter)).intValue()));
        }
        // An insertion sort: facet lists are short.
        for (int i = 1; i < out.size(); i++) {
            NameCountDto item = out.get(i);
            int j = i - 1;
            while (j >= 0 && before(item, out.get(j))) {
                out.set(j + 1, out.get(j));
                j--;
            }
            out.set(j + 1, item);
        }
        return out;
    }

    private static boolean before(NameCountDto a, NameCountDto b) {
        if (a.count() != b.count()) {
            return a.count() > b.count();
        }
        return a.name().compareTo(b.name()) < 0;
    }

    static long[] priceRange(List<Variant> variants) {
        long lo = Long.MAX_VALUE;
        long hi = 0;
        for (int iter = 0; iter < variants.size(); iter++) {
            long price = variants.get(iter).getPriceCents();
            lo = Math.min(lo, price);
            hi = Math.max(hi, price);
        }
        return new long[] {lo == Long.MAX_VALUE ? 0 : lo, hi};
    }

    static Map<String, List<Variant>> byProduct(List<Variant> variants) {
        Map<String, List<Variant>> out = new HashMap<String, List<Variant>>();
        for (int iter = 0; iter < variants.size(); iter++) {
            Variant variant = variants.get(iter);
            List<Variant> list = out.get(variant.getProductId());
            if (list == null) {
                list = new ArrayList<Variant>();
                out.put(variant.getProductId(), list);
            }
            list.add(variant);
        }
        return out;
    }

    /// The product's title in the shopper's language, or null.
    String translatedTitle(Product product) {
        return titles == null ? null : titles.get(product.getId());
    }

    /// The merchant's synonym groups ([#synonymGroups]); empty for none.
    List<String[]> synonyms = new ArrayList<String[]>();

    /// `word` and every word that shares a synonym group with it.
    String[] alternatives(String word) {
        for (int g = 0; g < synonyms.size(); g++) {
            String[] group = synonyms.get(g);
            for (int w = 0; w < group.length; w++) {
                if (group[w].equalsIgnoreCase(word)) {
                    String[] out = new String[group.length + 1];
                    out[0] = word;
                    System.arraycopy(group, 0, out, 1, group.length);
                    return out;
                }
            }
        }
        return new String[] {word};
    }

    /// Parses a store's synonym text: one group a line, words split by
    /// commas, blanks dropped, groups of one word ignored.
    public static List<String[]> synonymGroups(String text) {
        List<String[]> out = new ArrayList<String[]>();
        if (text == null) {
            return out;
        }
        List<String> group = new ArrayList<String>();
        int start = 0;
        for (int iter = 0; iter <= text.length(); iter++) {
            char c = iter == text.length() ? '\n' : text.charAt(iter);
            if (c == ',' || c == '\n') {
                String w = text.substring(start, iter).trim();
                if (w.length() > 0) {
                    group.add(w);
                }
                start = iter + 1;
                if (c == '\n') {
                    if (group.size() > 1) {
                        out.add(group.toArray(new String[group.size()]));
                    }
                    group.clear();
                }
            }
        }
        return out;
    }

    static boolean contains(String hay, String needle) {

        if (hay == null) {
            return false;
        }
        int n = needle.length();
        for (int iter = 0; iter + n <= hay.length(); iter++) {
            if (hay.regionMatches(true, iter, needle, 0, n)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWith(String hay, String needle) {
        return hay != null && hay.regionMatches(true, 0, needle, 0, needle.length());
    }

    private static String[] words(String q) {
        List<String> out = new ArrayList<String>();
        String text = text(q);
        int start = -1;
        for (int iter = 0; iter <= text.length(); iter++) {
            boolean space = iter == text.length() || text.charAt(iter) == ' ';
            if (space && start >= 0) {
                out.add(text.substring(start, iter));
                start = -1;
            } else if (!space && start < 0) {
                start = iter;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    static String text(String value) {
        return value == null ? "" : value.trim();
    }

    static long number(String value, long fallback) {
        String text = text(value);
        if (text.length() == 0 || text.length() > 15) {
            return fallback;
        }
        long n = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return fallback;
            }
            n = n * 10 + (c - '0');
        }
        return n;
    }
}
