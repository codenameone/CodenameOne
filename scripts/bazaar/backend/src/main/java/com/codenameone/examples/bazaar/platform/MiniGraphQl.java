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
package com.codenameone.examples.bazaar.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The small, read-only subset of GraphQL the key API reads: one query of
/// nested field selections over the JSON the REST routes answer.
///
/// ```
/// { products(first: 5) { id title variants { sku priceCents } } }
/// query Recent { latest: orders(first: 2) { number totalCents } }
/// ```
///
/// Read: the optional `query` keyword and name, fields, aliases (`a: b`) and
/// integer arguments. Refused, with a GraphQL error: mutations, variables,
/// fragments, directives, string arguments, and a field the objects do not
/// have. There is no schema to introspect; the fields are the REST
/// resources' members, so the REST documentation is the schema.
///
/// Pure, so it is tested without a server.
public final class MiniGraphQl {
    /// How deep selections may nest, and how many fields a query may name.
    static final int MAX_DEPTH = 8;
    static final int MAX_FIELDS = 200;

    private MiniGraphQl() {
    }

    /// A field of a selection: its name, the key it is answered under, its
    /// integer arguments and its own selection.
    public static final class Field {
        public final String name;
        public final String alias;
        public final Map<String, Integer> args = new LinkedHashMap<String, Integer>();
        public final List<Field> children = new ArrayList<Field>();

        Field(String name, String alias) {
            this.name = name;
            this.alias = alias;
        }

        public int intArg(String arg, int fallback) {
            Integer v = args.get(arg);
            return v == null ? fallback : v.intValue();
        }
    }

    /// `{"errors":[{"message":...}]}`.
    public static Map<String, Object> error(String message) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("message", message);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("errors", Collections.singletonList(m));
        return out;
    }

    /// The query's root selection; throws IllegalArgumentException, with a
    /// message for the client, for anything outside the subset.
    public static Field parse(String query) {
        Parser p = new Parser(query == null ? "" : query);
        p.skip();
        if (p.word("mutation") || p.word("subscription")) {
            throw new IllegalArgumentException("Only queries are supported");
        }
        if (p.word("query")) {
            p.pos += 5;
            p.skip();
            if (p.peek() != '{' && p.peek() != '(') {
                p.name();
                p.skip();
            }
            if (p.peek() == '(') {
                throw new IllegalArgumentException("Variables are not supported");
            }
        }
        Field root = new Field("", "");
        p.selection(root, 0);
        p.skip();
        if (p.pos < p.text.length()) {
            throw new IllegalArgumentException("Only one operation per request");
        }
        if (root.children.isEmpty()) {
            throw new IllegalArgumentException("Select at least one field");
        }
        return root;
    }

    /// `value` cut down to the fields `selection` asks for; lists are
    /// projected element by element.
    public static Object project(Object value, Field selection) {
        if (selection.children.isEmpty()) {
            return value;
        }
        if (value instanceof List) {
            List list = (List) value;
            List<Object> out = new ArrayList<Object>();
            for (int iter = 0; iter < list.size(); iter++) {
                out.add(project(list.get(iter), selection));
            }
            return out;
        }
        if (!(value instanceof Map)) {
            return null;
        }
        Map source = (Map) value;
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (int iter = 0; iter < selection.children.size(); iter++) {
            Field f = selection.children.get(iter);
            if (!source.containsKey(f.name)) {
                throw new IllegalArgumentException("No field " + f.name);
            }
            out.put(f.alias, project(source.get(f.name), f));
        }
        return out;
    }

    private static final class Parser {
        final String text;
        int pos;
        int fields;

        Parser(String text) {
            this.text = text;
        }

        char peek() {
            return pos < text.length() ? text.charAt(pos) : '\0';
        }

        void skip() {
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == '#') {
                    while (pos < text.length() && text.charAt(pos) != '\n') {
                        pos++;
                    }
                } else if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == ',') {
                    pos++;
                } else {
                    return;
                }
            }
        }

        boolean word(String w) {
            if (!text.startsWith(w, pos)) {
                return false;
            }
            int end = pos + w.length();
            return end >= text.length() || !isNameChar(text.charAt(end));
        }

        static boolean isNameChar(char c) {
            return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9');
        }

        String name() {
            int start = pos;
            while (pos < text.length() && isNameChar(text.charAt(pos))) {
                pos++;
            }
            if (start == pos || (text.charAt(start) >= '0' && text.charAt(start) <= '9')) {
                throw new IllegalArgumentException("Expected a name at " + start);
            }
            return text.substring(start, pos);
        }

        void expect(char c) {
            skip();
            if (peek() != c) {
                throw new IllegalArgumentException("Expected '" + c + "' at " + pos);
            }
            pos++;
        }

        void selection(Field parent, int depth) {
            if (depth >= MAX_DEPTH) {
                throw new IllegalArgumentException("The query nests too deeply");
            }
            expect('{');
            while (true) {
                skip();
                char c = peek();
                if (c == '}') {
                    pos++;
                    return;
                }
                if (c == '\0') {
                    throw new IllegalArgumentException("Unclosed selection");
                }
                if (c == '.' || c == '@' || c == '$') {
                    throw new IllegalArgumentException("Fragments, directives and variables "
                            + "are not supported");
                }
                if (++fields > MAX_FIELDS) {
                    throw new IllegalArgumentException("The query names too many fields");
                }
                String first = name();
                skip();
                Field f;
                if (peek() == ':') {
                    pos++;
                    skip();
                    f = new Field(name(), first);
                    skip();
                } else {
                    f = new Field(first, first);
                }
                if (peek() == '(') {
                    pos++;
                    arguments(f);
                    skip();
                }
                if (peek() == '{') {
                    selection(f, depth + 1);
                }
                parent.children.add(f);
            }
        }

        void arguments(Field f) {
            while (true) {
                skip();
                if (peek() == ')') {
                    pos++;
                    return;
                }
                String arg = name();
                expect(':');
                skip();
                int start = pos;
                if (peek() == '-') {
                    pos++;
                }
                while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9') {
                    pos++;
                }
                String digits = text.substring(start, pos);
                if (digits.length() == 0 || "-".equals(digits) || digits.length() > 9) {
                    throw new IllegalArgumentException("Argument " + arg
                            + " must be a whole number");
                }
                f.args.put(arg, Integer.valueOf(Integer.parseInt(digits)));
            }
        }
    }
}
